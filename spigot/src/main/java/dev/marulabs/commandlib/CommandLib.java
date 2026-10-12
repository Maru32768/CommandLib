package dev.marulabs.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import dev.marulabs.commandlib.util.bukkit.BukkitUtil;
import dev.marulabs.commandlib.util.bukkit.MinecraftVersion;
import dev.marulabs.commandlib.util.nms.command.NMSCommandDispatcher;
import dev.marulabs.commandlib.util.nms.command.NMSVanillaCommandWrapper;
import dev.marulabs.commandlib.util.nms.server.NMSCraftServer;
import dev.marulabs.commandlib.util.nms.server.NMSDedicatedServer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.help.GenericCommandHelpTopic;
import org.bukkit.help.HelpMap;
import org.bukkit.help.HelpTopic;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

public final class CommandLib implements Listener {
    private final Plugin plugin;
    private final Collection<? extends Command> commands;
    private final String permissionPrefix;
    private final List<CommandNode<?>> registeredCommands = new ArrayList<>();
    private final List<Permission> registeredPermissions = new ArrayList<>();
    private final List<HelpTopic> registeredHelpTopics = new ArrayList<>();
    private static @Nullable ServerLoadEvent functionsRecompiledFor;

    public static CommandLib register(@NotNull Plugin plugin, @NotNull Command command, @NotNull Command... commands) {
        List<Command> list = new ArrayList<>();
        list.add(command);
        Collections.addAll(list, commands);
        return register(plugin, list);
    }

    public static CommandLib register(@NotNull Plugin plugin, @NotNull Collection<Command> commands) {
        return new CommandLib(plugin,
                              commands,
                              plugin.getName()
                                    .toLowerCase(Locale.ROOT) + ".command");
    }

    public static CommandLib register(@NotNull Plugin plugin,
                                      @NotNull String permissionPrefix,
                                      @NotNull Command command,
                                      @NotNull Command... commands) {
        List<Command> list = new ArrayList<>();
        list.add(command);
        Collections.addAll(list, commands);
        return register(plugin, permissionPrefix, list);
    }

    public static CommandLib register(@NotNull Plugin plugin,
                                      @NotNull String permissionPrefix,
                                      @NotNull Collection<? extends Command> commands) {
        return new CommandLib(plugin, commands, permissionPrefix);
    }

    private CommandLib(Plugin plugin, Collection<? extends Command> commands, String permissionPrefix) {
        this.plugin = Objects.requireNonNull(plugin);
        this.commands = Objects.requireNonNull(commands);
        this.permissionPrefix = Objects.requireNonNull(permissionPrefix);
        if (permissionPrefix.isEmpty()) {
            throw new IllegalArgumentException("permissionPrefix must not be empty");
        }
        for (Command command : commands) {
            Objects.requireNonNull(command);
        }

        try {
            enable();
        } catch (RuntimeException e) {
            // The disable listener is not registered yet, so nothing else would remove what was added so far.
            try {
                unregister(false);
            } catch (RuntimeException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
        Bukkit.getPluginManager()
              .registerEvents(this, plugin);
        registerResourcesReloadListener();
    }

    /**
     * From 1.20.6 the commands are added straight to Paper's dispatcher, which /minecraft:reload replaces. Paper
     * fires ServerResourcesReloadedEvent after re-syncing Bukkit commands, so the nodes are added again there. The
     * event is Paper API, so it is registered reflectively.
     */
    @SuppressWarnings("unchecked")
    private void registerResourcesReloadListener() {
        if (!usesPaperCommandDispatcher()) {
            return;
        }
        Class<? extends Event> eventClass;
        try {
            eventClass = (Class<? extends Event>) Class.forName("io.papermc.paper.event.server.ServerResourcesReloadedEvent");
        } catch (ClassNotFoundException e) {
            return;
        }
        EventExecutor executor = (listener, event) -> {
            if (eventClass.isInstance(event)) {
                ((CommandLib) listener).readdToPaperDispatcher();
            }
        };
        Bukkit.getPluginManager()
              .registerEvent(eventClass, this, EventPriority.MONITOR, executor, plugin);
    }

    private void readdToPaperDispatcher() {
        try {
            addToPaperDispatcher(knownCommands(), registeredCommands);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        updatePlayerCommandsLater();
    }

    /**
     * Paper replaces the Bukkit command map with one backed by its Brigadier dispatcher from 1.20.6. Spigot keeps the
     * vanilla dispatcher, which takes commands through the NMS bridge as before.
     */
    private static boolean usesPaperCommandDispatcher() {
        if (new MinecraftVersion(BukkitUtil.getMinecraftVersion()).isLessThan(new MinecraftVersion("1.20.6"))) {
            return false;
        }
        try {
            Class.forName("io.papermc.paper.command.brigadier.ShadowBrigNode");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, org.bukkit.command.Command> knownCommands() throws Exception {
        CommandMap commandMap = NMSCraftServer.create()
                                              .getCommandMap();
        Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
        knownCommandsField.setAccessible(true);
        return ((Map<String, org.bukkit.command.Command>) knownCommandsField.get(commandMap));
    }

    @SuppressWarnings("rawtypes")
    private static RootCommandNode paperDispatcherRoot(Map<String, org.bukkit.command.Command> knownCommands) throws Exception {
        CommandDispatcher dispatcher = ((CommandDispatcher) knownCommands.getClass()
                                                                         .getDeclaredMethod("getDispatcher")
                                                                         .invoke(knownCommands));
        return dispatcher.getRoot();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addToPaperDispatcher(Map<String, org.bukkit.command.Command> knownCommands,
                                             List<CommandNode<?>> nodes) throws Exception {
        RootCommandNode root = paperDispatcherRoot(knownCommands);
        Constructor<?> shadowBrigNode = Class.forName("io.papermc.paper.command.brigadier.ShadowBrigNode")
                                             .getConstructor(CommandNode.class);
        for (CommandNode<?> node : nodes) {
            removeRegisteredCommand(root, knownCommands, node.getName());
            root.addChild((CommandNode) shadowBrigNode.newInstance(node));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void enable() {
        // Build first, so invalid commands are rejected before any permission is added to the server.
        List<CommandNode<?>> nodes = new ArrayList<>(new CommandNodeCreator<>(commands, permissionPrefix).build());
        registerPermissions();

        registeredCommands.addAll(nodes);
        try {
            Map<String, org.bukkit.command.Command> knownCommands = knownCommands();
            if (usesPaperCommandDispatcher()) {
                addToPaperDispatcher(knownCommands, registeredCommands);
            } else {
                NMSDedicatedServer server = NMSCraftServer.create(plugin.getServer())
                                                          .getServer();
                // Commands live in the vanilla dispatcher, which "execute run" and data pack functions use and which
                // is never replaced, and in the synced one players see, unless both are the same during startup.
                NMSCommandDispatcher vanillaDispatcher = server.getVanillaCommandDispatcher();
                RootCommandNode vanillaRoot = vanillaDispatcher.getBrigadier()
                                                               .getRoot();
                RootCommandNode root = server.getCommandDispatcher()
                                             .getBrigadier()
                                             .getRoot();
                CommandMap commandMap = NMSCraftServer.create(plugin.getServer())
                                                      .getCommandMap();
                Map<String, Command> commandsByLabel = commandsByLabel(commands);
                for (CommandNode x : registeredCommands) {
                    removeRegisteredCommand(root, knownCommands, x.getName());
                    removeCommand(vanillaRoot, x.getName());

                    vanillaRoot.addChild(x);
                    if (root != vanillaRoot) {
                        root.addChild(x);
                    }
                    BukkitCommand wrapper = NMSVanillaCommandWrapper.create()
                                                                    .createInstance(vanillaDispatcher, x);
                    describe(wrapper,
                             Objects.requireNonNull(commandsByLabel.get(x.getName())),
                             x,
                             permissionPrefix,
                             plugin.getName());
                    // Marks the wrapper as registered, as SimpleCommandMap#register does; help topics hide others.
                    wrapper.register(commandMap);
                    knownCommands.put(x.getName(), wrapper);
                    HelpTopic helpTopic = new GenericCommandHelpTopic(wrapper);
                    putHelpTopic(Bukkit.getHelpMap(), helpTopic);
                    registeredHelpTopics.add(helpTopic);

                    // When the synced dispatcher is the vanilla one, CraftBukkit has not synced the commands yet and
                    // adds the namespaced label to the synced dispatcher from the command map itself.
                    addNamespacedCommand(root == vanillaRoot ? null
                            : root, knownCommands, namespace(plugin), x, wrapper);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        updatePlayerCommandsLater();
    }

    private static Map<String, Command> commandsByLabel(Collection<? extends Command> commands) {
        Map<String, Command> map = new HashMap<>();
        for (Command command : commands) {
            map.put(command.name(), command);
            command.aliases()
                   .forEach(alias -> map.put(alias, command));
        }
        return map;
    }

    /**
     * Replaces what VanillaCommandWrapper reports for a Mojang command ("A Mojang provided command.", the
     * minecraft.command.&lt;name&gt; permission) with the command's own description, usage and permission, which
     * /help and other plugins read. The Brigadier requirement still checks the permission when the command runs.
     */
    static void describe(BukkitCommand wrapper,
                         Command command,
                         CommandNode<?> node,
                         String permissionPrefix,
                         String pluginName) {
        wrapper.setPermission(command.permissionName(permissionPrefix));
        String description = command.staticDescription();
        wrapper.setDescription(description == null || description.isEmpty()
                ? "A command by the " + pluginName + " plugin." : description);
        wrapper.setUsage(usage(node));
        List<String> otherLabels = new ArrayList<>();
        otherLabels.add(command.name());
        otherLabels.addAll(command.aliases());
        otherLabels.remove(node.getName());
        wrapper.setAliases(otherLabels);
    }

    private static String usage(CommandNode<?> node) {
        List<String> lines = new ArrayList<>();
        if (node.getCommand() != null) {
            lines.add("/<command>");
        }
        node.getChildren()
            .forEach(child -> lines.add("/<command> " + child.getUsageText()));
        if (lines.size() == 1) {
            return lines.get(0);
        }
        StringBuilder sb = new StringBuilder();
        lines.forEach(line -> sb.append("\n- ")
                                .append(line));
        return sb.toString();
    }

    /**
     * HelpMap#addTopic keeps an existing topic of the same name, so that one is removed first, as the command map
     * entry it described is replaced as well. CraftBukkit's getHelpTopics() is a live view of its topics.
     */
    static void putHelpTopic(HelpMap helpMap, HelpTopic topic) {
        HelpTopic existing = helpMap.getHelpTopic(topic.getName());
        if (existing != null) {
            helpMap.getHelpTopics()
                   .remove(existing);
        }
        helpMap.addTopic(topic);
    }

    static void removeHelpTopic(HelpMap helpMap, HelpTopic topic) {
        if (helpMap.getHelpTopic(topic.getName()) == topic) {
            helpMap.getHelpTopics()
                   .remove(topic);
        }
    }

    /**
     * Data pack functions are compiled when the data packs load, before plugins add their commands, and a function
     * with an unknown command is dropped. Once every plugin has been enabled, the functions are compiled again against
     * the vanilla dispatcher, as CommandAPI does. Every CommandLib instance of a plugin sees the same event, so only
     * the first one compiles. A later /minecraft:reload compiles functions against fresh vanilla commands again, and
     * Spigot fires no event for it.
     */
    @EventHandler
    private void onServerLoad(ServerLoadEvent e) {
        if (functionsRecompiledFor == e || usesPaperCommandDispatcher()) {
            return;
        }
        functionsRecompiledFor = e;
        try {
            NMSDedicatedServer server = NMSCraftServer.create(plugin.getServer())
                                                      .getServer();
            server.recompileFunctions(server.getVanillaCommandDispatcher());
        } catch (RuntimeException | LinkageError ex) {
            plugin.getLogger()
                  .log(Level.WARNING, "Could not compile data pack functions again with CommandLib's commands", ex);
        }
    }

    @EventHandler
    private void onPluginDisable(PluginDisableEvent e) {
        if (!e.getPlugin()
              .equals(plugin)) {
            return;
        }

        unregister(false);
    }

    public void unregister() {
        unregister(true);
    }

    // PluginDisableEvent is fired while the server/plugin manager is already tearing down the plugin.
    // On older Bukkit/Paper versions, sending command updates during that path can race with async
    // command-tree serialization and cause ConcurrentModificationException. Runtime unregisters still
    // need to refresh online players, so keep the update controlled by this flag.
    private void unregister(boolean updatePlayerCommands) {
        try {
            unregisterCommands();
        } finally {
            HandlerList.unregisterAll(this);
            unregisterPermissions();
            registeredHelpTopics.forEach(topic -> removeHelpTopic(Bukkit.getHelpMap(), topic));
            registeredHelpTopics.clear();
            registeredCommands.clear();
            registeredPermissions.clear();

            if (updatePlayerCommands) {
                updatePlayerCommandsLater();
            }
        }
    }

    private void registerPermissions() {
        // removePermissionCompletely guards against leftover permissions if the previous
        // disable cycle's cleanup was incomplete. addPermission(TRUE) automatically calls
        // dirtyPermissibles internally, so no explicit recalculatePermissions is needed here.
        commands.stream()
                .flatMap(x -> x.permissions(permissionPrefix)
                               .stream())
                .forEach(permission -> {
                    removePermissionCompletely(permission.getName());
                    Bukkit.getPluginManager()
                          .addPermission(permission);
                    registeredPermissions.add(permission);
                });
    }

    private void unregisterPermissions() {
        registeredPermissions.stream()
                             .map(Permission::getName)
                             .forEach(this::removePermissionCompletely);
        // dirtyPermissibles is not called when removing a TRUE permission, so force
        // recalculation here so players lose the permission immediately.
        Bukkit.getOnlinePlayers()
              .forEach(Permissible::recalculatePermissions);
    }

    private void removePermissionCompletely(String name) {
        // removePermission only removes from the permissions map and leaves defaultPerms intact.
        // Changing the default to FALSE and calling recalculatePermissionDefaults first ensures
        // the stale TRUE entry is evicted from defaultPerms; otherwise an ALL->NONE change does
        // not take effect after a plugin reload.
        Permission existing = Bukkit.getPluginManager()
                                    .getPermission(name);
        if (existing != null) {
            existing.setDefault(org.bukkit.permissions.PermissionDefault.FALSE);
            Bukkit.getPluginManager()
                  .recalculatePermissionDefaults(existing);
        }
        Bukkit.getPluginManager()
              .removePermission(name);
    }

    private void updatePlayerCommandsLater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getOnlinePlayers()
                      .forEach(Player::updateCommands);
            }
        }.runTask(plugin);
    }

    @SuppressWarnings("rawtypes")
    private void unregisterCommands() {
        try {
            Map<String, org.bukkit.command.Command> knownCommands = knownCommands();
            boolean usePaperCommandDispatcher = usesPaperCommandDispatcher();
            RootCommandNode root;
            RootCommandNode vanillaRoot = null;
            if (usePaperCommandDispatcher) {
                root = paperDispatcherRoot(knownCommands);
            } else {
                NMSDedicatedServer server = NMSCraftServer.create(plugin.getServer())
                                                          .getServer();
                root = server.getCommandDispatcher()
                             .getBrigadier()
                             .getRoot();
                vanillaRoot = server.getVanillaCommandDispatcher()
                                    .getBrigadier()
                                    .getRoot();
            }

            for (CommandNode<?> node : registeredCommands) {
                removeRegisteredCommand(root, knownCommands, node.getName());

                if (!usePaperCommandDispatcher) {
                    removeCommand(Objects.requireNonNull(vanillaRoot), node.getName());
                    removeMinecraftNamespacedCommand(root, knownCommands, node);
                    removeRegisteredCommand(root, knownCommands, namespace(plugin) + ":" + node.getName());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String namespace(Plugin plugin) {
        return plugin.getName()
                     .toLowerCase(Locale.ROOT);
    }

    /**
     * Adds "&lt;plugin&gt;:&lt;name&gt;" as another label of the wrapper, like Bukkit's fallback labels.
     * VanillaCommandWrapper parses its node's own name, so the label needs no node in the vanilla dispatcher. The
     * synced dispatcher, which players see, gets a copy of the node under the label, as CraftServer#syncCommands
     * makes for such labels.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void addNamespacedCommand(@Nullable RootCommandNode resourcesRoot,
                                     Map<String, org.bukkit.command.Command> knownCommands,
                                     String namespace,
                                     CommandNode<?> node,
                                     org.bukkit.command.Command wrapper) throws Exception {
        String label = namespace + ":" + node.getName();
        knownCommands.put(label, wrapper);
        if (resourcesRoot == null) {
            return;
        }
        removeCommand(resourcesRoot, label);
        LiteralCommandNode copy = new LiteralCommandNode(label,
                                                         node.getCommand(),
                                                         node.getRequirement(),
                                                         node.getRedirect(),
                                                         node.getRedirectModifier(),
                                                         node.isFork());
        node.getChildren()
            .forEach(child -> copy.addChild((CommandNode) child));
        resourcesRoot.addChild(copy);
    }

    /**
     * When the server finishes loading, CraftBukkit adds every child of the vanilla dispatcher to the command map as
     * "minecraft:&lt;name&gt;". Commands registered during startup are children of it then, so that label is removed
     * as well. Only a wrapper of this node is removed, so the label of another command stays.
     */
    static void removeMinecraftNamespacedCommand(RootCommandNode<?> root,
                                                 Map<String, org.bukkit.command.Command> knownCommands,
                                                 CommandNode<?> node) throws Exception {
        String label = "minecraft:" + node.getName();
        org.bukkit.command.Command command = knownCommands.get(label);
        if (command == null || wrappedVanillaCommand(command) != node) {
            return;
        }
        knownCommands.remove(label);
        removeCommand(root, label);
    }

    @Nullable
    private static Object wrappedVanillaCommand(org.bukkit.command.Command command) throws IllegalAccessException {
        try {
            // VanillaCommandWrapper#vanillaCommand is public CraftBukkit API, which keeps its name on every version.
            return command.getClass()
                          .getField("vanillaCommand")
                          .get(command);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static void removeRegisteredCommand(RootCommandNode<?> root,
                                                Map<String, org.bukkit.command.Command> knownCommands,
                                                String name) throws Exception {
        removeCommand(root, name);
        knownCommands.remove(name);
    }

    @SuppressWarnings("rawtypes")
    private static void removeCommand(CommandNode<?> commandNode, String name) throws Exception {
        if (commandNode == null) {
            return;
        }

        Class<?> clazz = CommandNode.class;

        Field children = clazz.getDeclaredField("children");
        children.setAccessible(true);
        ((Map) children.get(commandNode)).remove(name);

        Field literals = clazz.getDeclaredField("literals");
        literals.setAccessible(true);
        ((Map) literals.get(commandNode)).remove(name);

        Field arguments = clazz.getDeclaredField("arguments");
        arguments.setAccessible(true);
        ((Map) arguments.get(commandNode)).remove(name);
    }
}
