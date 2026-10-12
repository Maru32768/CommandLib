package dev.marulabs.commandlib;

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
import org.bukkit.help.HelpTopicComparator;
import org.bukkit.help.IndexHelpTopic;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.logging.Level;

public final class CommandLib implements Listener {
    private final Plugin plugin;
    private final Collection<? extends Command> commands;
    private final String permissionPrefix;
    private final Map<String, Command> commandsByLabel;
    private final List<CommandNode<?>> registeredCommands = new ArrayList<>();
    private final List<Permission> registeredPermissions = new ArrayList<>();
    private final List<HelpTopic> registeredHelpTopics = new ArrayList<>();
    // Shared by the instances of this copy of CommandLib, which a plugin shades.
    private static @Nullable BukkitTask functionCompiler;
    private static @Nullable Object functionsCompiledFor;
    private static boolean commandsChangedSinceCompile;

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
        this.commandsByLabel = commandsByLabel(commands);

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
     * From 1.20.6 the commands are added straight to Paper's dispatcher, which /minecraft:reload replaces, moving only
     * Bukkit commands into the new one. The nodes are added again from a COMMANDS lifecycle handler, which Paper runs
     * before it sends the new tree to players. Paper takes such a handler only while the plugin is enabling, so a
     * later registration adds them on ServerResourcesReloadedEvent instead, after the trees are sent, and the update on
     * the next tick sends them again with the commands. The trees are built on Paper's asynchronous pool, so the nodes
     * are added without changing the maps a build may be iterating ({@link #addToPaperDispatcher}). The help topics,
     * which the reload rebuilt without the commands, are put on that event in either case. Both are Paper API, so they
     * are registered reflectively.
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
            // usesPaperCommandDispatcher() means Paper 1.20.6+, which always has this event.
            throw new IllegalStateException("Paper's ServerResourcesReloadedEvent is missing", e);
        }
        boolean commandsHandler = registerCommandsHandler();
        EventExecutor executor = (listener, event) -> {
            if (eventClass.isInstance(event)) {
                ((CommandLib) listener).afterResourcesReload(!commandsHandler);
            }
        };
        Bukkit.getPluginManager()
              .registerEvent(eventClass, this, EventPriority.MONITOR, executor, plugin);
    }

    private boolean registerCommandsHandler() {
        try {
            Class<?> managerClass = Class.forName("io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager");
            Class<?> typeClass = Class.forName("io.papermc.paper.plugin.lifecycle.event.types.LifecycleEventType");
            Class<?> handlerClass = Class.forName("io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler");
            Object commandsEvent = Class.forName("io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents")
                                        .getField("COMMANDS")
                                        .get(null);
            Object handler = Proxy.newProxyInstance(handlerClass.getClassLoader(),
                                                    new Class<?>[]{handlerClass},
                                                    (proxy, method, args) -> {
                                                        switch (method.getName()) {
                                                            case "equals":
                                                                return proxy == args[0];
                                                            case "hashCode":
                                                                return System.identityHashCode(proxy);
                                                            case "toString":
                                                                return "CommandLib COMMANDS handler";
                                                            default:
                                                                readdToPaperDispatcher();
                                                                return null;
                                                        }
                                                    });
            Object manager = plugin.getClass()
                                   .getMethod("getLifecycleManager")
                                   .invoke(plugin);
            managerClass.getMethod("registerEventHandler", typeClass, handlerClass)
                        .invoke(manager, commandsEvent, handler);
            return true;
        } catch (InvocationTargetException e) {
            // Paper rejects a handler once the plugin has been enabled.
            return false;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void readdToPaperDispatcher() {
        // A lifecycle handler cannot be removed, so it outlives unregister().
        if (registeredCommands.isEmpty()) {
            return;
        }
        try {
            addToPaperDispatcher();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void afterResourcesReload(boolean readd) {
        if (readd) {
            readdToPaperDispatcher();
            updatePlayerCommandsLater();
        }
        try {
            showPaperHelpTopics(knownCommands());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    /**
     * Adds each node under its label and under "&lt;plugin&gt;:&lt;label&gt;", as Paper's registrar does. Paper shows a
     * node as a Bukkit command through the API node cached on it: a PluginCommandNode becomes a
     * PluginVanillaCommandWrapper, which names the plugin and the description and has no permission of its own, while
     * any other node becomes a VanillaCommandWrapper, "A Mojang provided command." with a minecraft.command.&lt;label&gt;
     * permission that Bukkit's dispatchCommand checks. The API node gets no children, so Paper's usage of it stays the
     * label instead of testing the requirements of the children. Paper builds players' command trees on an
     * asynchronous pool by iterating the root's children, so the nodes replace the root's maps instead of changing
     * them ({@link CommandNodeChildren}).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addToPaperDispatcher() throws Exception {
        // Paper's live dispatcher, which its command map forwards to.
        RootCommandNode root = NMSCraftServer.create(plugin.getServer())
                                             .getServer()
                                             .getCommandDispatcher()
                                             .getBrigadier()
                                             .getRoot();
        Constructor<?> pluginCommandNode = Class.forName("io.papermc.paper.command.brigadier.PluginCommandNode")
                                                .getConstructor(String.class,
                                                                Class.forName("io.papermc.paper.plugin.configuration.PluginMeta"),
                                                                LiteralCommandNode.class,
                                                                String.class);
        Object pluginMeta = plugin.getClass()
                                  .getMethod("getPluginMeta")
                                  .invoke(plugin);
        Field wrappedCached = CommandNode.class.getField("wrappedCached");
        Field unwrappedCached = CommandNode.class.getField("unwrappedCached");
        List<String> labels = new ArrayList<>();
        List<CommandNode<?>> labelNodes = new ArrayList<>();
        for (CommandNode<?> node : registeredCommands) {
            String description = Objects.requireNonNull(commandsByLabel.get(node.getName()))
                                        .serverHelpShortText(plugin.getName());
            for (CommandNode<?> labelNode : Arrays.asList(node, copy(node, namespace(plugin) + ":" + node.getName()))) {
                Object apiNode = pluginCommandNode.newInstance(labelNode.getName(),
                                                               pluginMeta,
                                                               new LiteralCommandNode(labelNode.getName(),
                                                                                      labelNode.getCommand(),
                                                                                      labelNode.getRequirement(),
                                                                                      null,
                                                                                      null,
                                                                                      false),
                                                               description);
                wrappedCached.set(labelNode, apiNode);
                unwrappedCached.set(apiNode, labelNode);
                labels.add(labelNode.getName());
                labelNodes.add(labelNode);
            }
        }
        CommandNodeChildren.replace(root, labels, labelNodes);
    }

    /**
     * Paper builds the help map from the command map when the server loads and on every /minecraft:reload, so a
     * registration at another time, or a reload, which drops the nodes, leaves the commands without topics. Paper's
     * wrapper for a label is made on each lookup and has no permission, so it gets the command permission for the
     * topic, which then shows only to those who may run the command.
     */
    private void showPaperHelpTopics(Map<String, org.bukkit.command.Command> knownCommands) {
        registeredHelpTopics.clear();
        for (CommandNode<?> node : registeredCommands) {
            Command command = Objects.requireNonNull(commandsByLabel.get(node.getName()));
            for (String label : Arrays.asList(node.getName(), namespace(plugin) + ":" + node.getName())) {
                org.bukkit.command.Command wrapper = knownCommands.get(label);
                if (wrapper == null) {
                    continue;
                }
                wrapper.setPermission(command.permissionName(permissionPrefix));
                HelpTopic topic = helpTopic(wrapper, command);
                putHelpTopic(Bukkit.getHelpMap(), topic, plugin.getName());
                registeredHelpTopics.add(topic);
            }
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
                addToPaperDispatcher();
                showPaperHelpTopics(knownCommands);
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
                for (CommandNode x : registeredCommands) {
                    // Paper before 1.20.6 builds players' command trees asynchronously from the synced root.
                    knownCommands.remove(x.getName());
                    List<String> names = Collections.singletonList(x.getName());
                    List<CommandNode<?>> added = Collections.singletonList(x);
                    CommandNodeChildren.replace(vanillaRoot, names, added);
                    if (root != vanillaRoot) {
                        CommandNodeChildren.replace(root, names, added);
                    }
                    BukkitCommand wrapper = NMSVanillaCommandWrapper.create()
                                                                    .createInstance(vanillaDispatcher, x);
                    Command command = Objects.requireNonNull(commandsByLabel.get(x.getName()));
                    describe(wrapper, command, x, permissionPrefix, plugin.getName());
                    // Marks the wrapper as registered, as SimpleCommandMap#register does; help topics hide others.
                    wrapper.register(commandMap);
                    knownCommands.put(x.getName(), wrapper);
                    HelpTopic helpTopic = helpTopic(wrapper, command);
                    putHelpTopic(Bukkit.getHelpMap(), helpTopic, plugin.getName());
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
        compileFunctionsWithCommands();
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
     * minecraft.command.&lt;name&gt; permission) with the command's own server help text, usage and permission, which
     * /help and other plugins read. The Brigadier requirement still checks the permission when the command runs.
     */
    static void describe(BukkitCommand wrapper,
                         Command command,
                         CommandNode<?> node,
                         String permissionPrefix,
                         String pluginName) {
        wrapper.setPermission(command.permissionName(permissionPrefix));
        wrapper.setDescription(command.serverHelpShortText(pluginName));
        wrapper.setUsage(usage(node));
        List<String> otherLabels = new ArrayList<>();
        otherLabels.add(command.name());
        otherLabels.addAll(command.aliases());
        otherLabels.remove(node.getName());
        wrapper.setAliases(otherLabels);
    }

    /**
     * The topic CraftBukkit would make for the wrapper, with the page set by {@link Command#serverHelp(String, String)}.
     */
    static HelpTopic helpTopic(org.bukkit.command.Command wrapper, Command command) {
        HelpTopic topic = new GenericCommandHelpTopic(wrapper);
        String fullText = command.serverHelpFullText();
        if (fullText != null) {
            topic.amendTopic(null, fullText);
        }
        return topic;
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
    static void putHelpTopic(HelpMap helpMap, HelpTopic topic, String pluginName) {
        HelpTopic existing = helpMap.getHelpTopic(topic.getName());
        if (existing != null) {
            helpMap.getHelpTopics()
                   .remove(existing);
        }
        helpMap.addTopic(topic);
        fileUnderPluginIndex(helpMap, topic, pluginName);
    }

    /**
     * CraftBukkit lists a VanillaCommandWrapper in the "Minecraft" index, since it picks the index by the command
     * class, so the topic is moved to the plugin's index, which is made when the plugin has none. The indexes exist
     * once the help map has been built after the plugins were enabled; before that, when the "Minecraft" index is
     * missing, the topic is left for ServerLoadEvent.
     */
    static void fileUnderPluginIndex(HelpMap helpMap, HelpTopic topic, String pluginName) {
        Collection<HelpTopic> minecraftTopics = indexTopics(helpMap.getHelpTopic("Minecraft"));
        if (minecraftTopics == null || helpMap.getHelpTopic(topic.getName()) != topic) {
            return;
        }
        // The index sets compare topics by name.
        minecraftTopics.remove(topic);
        HelpTopic index = helpMap.getHelpTopic(pluginName);
        Collection<HelpTopic> pluginTopics = indexTopics(index);
        if (pluginTopics == null) {
            if (index != null) {
                return;
            }
            pluginTopics = new TreeSet<>(HelpTopicComparator.helpTopicComparatorInstance());
            // The same texts as CraftBukkit's plugin indexes.
            helpMap.addTopic(new IndexHelpTopic(pluginName,
                                                "All commands for " + pluginName,
                                                null,
                                                pluginTopics,
                                                "Below is a list of all " + pluginName + " commands:"));
        }
        pluginTopics.remove(topic);
        pluginTopics.add(topic);
    }

    static void removeHelpTopic(HelpMap helpMap, HelpTopic topic, String pluginName) {
        if (helpMap.getHelpTopic(topic.getName()) != topic) {
            return;
        }
        helpMap.getHelpTopics()
               .remove(topic);
        HelpTopic index = helpMap.getHelpTopic(pluginName);
        Collection<HelpTopic> pluginTopics = indexTopics(index);
        if (pluginTopics != null) {
            pluginTopics.remove(topic);
            if (pluginTopics.isEmpty()) {
                helpMap.getHelpTopics()
                       .remove(index);
            }
        }
    }

    /**
     * The topics an index lists, which IndexHelpTopic keeps in a protected field without an accessor. CraftBukkit's
     * plugin indexes hold a mutable set there.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    static Collection<HelpTopic> indexTopics(@Nullable HelpTopic index) {
        if (!(index instanceof IndexHelpTopic)) {
            return null;
        }
        try {
            Field field = IndexHelpTopic.class.getDeclaredField("allTopics");
            field.setAccessible(true);
            return (Collection<HelpTopic>) field.get(index);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // The help map has its indexes once the server has loaded. Bukkit needs the event parameter.
    @SuppressWarnings("UnusedVariable")
    @EventHandler
    private void onServerLoad(ServerLoadEvent e) {
        registeredHelpTopics.forEach(topic -> fileUnderPluginIndex(Bukkit.getHelpMap(), topic, plugin.getName()));
        removeMinecraftNamespacedCommands();
    }

    /**
     * When the plugins have been enabled at startup or by Bukkit /reload, CraftBukkit adds every child of the vanilla
     * dispatcher to the command map as "minecraft:&lt;name&gt;", a VanillaCommandWrapper that /help leaves out and that
     * describes a Mojang command. CommandLib commands are plugin commands, which Bukkit labels "&lt;plugin&gt;:&lt;name&gt;"
     * only, as CommandAPI does for commands outside the minecraft namespace, so the label is removed again.
     */
    @SuppressWarnings("rawtypes")
    private void removeMinecraftNamespacedCommands() {
        if (usesPaperCommandDispatcher()) {
            return;
        }
        boolean removed = false;
        try {
            Map<String, org.bukkit.command.Command> knownCommands = knownCommands();
            RootCommandNode root = NMSCraftServer.create(plugin.getServer())
                                                 .getServer()
                                                 .getCommandDispatcher()
                                                 .getBrigadier()
                                                 .getRoot();
            for (CommandNode<?> node : registeredCommands) {
                removed |= removeMinecraftNamespacedCommand(root, knownCommands, node);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        // Bukkit /reload has sent the players their trees with the label.
        if (removed) {
            updatePlayerCommandsLater();
        }
    }

    /**
     * Data pack functions are compiled when the data packs load: at startup before plugins add their commands, and on
     * /minecraft:reload, /datapack or Bukkit#reloadData against fresh vanilla commands, for which Spigot fires no
     * event. A function with an unknown command is dropped. So a task checks every tick whether the data pack
     * resources were replaced or the commands changed, and then compiles the functions again with the dispatcher
     * that holds the commands. Bukkit tasks run before the server ticks functions, so the #load functions run once,
     * with the commands.
     */
    private void compileFunctionsWithCommands() {
        commandsChangedSinceCompile = true;
        if (functionCompiler == null || functionCompiler.isCancelled()) {
            NMSDedicatedServer server = NMSCraftServer.create(plugin.getServer())
                                                      .getServer();
            // Kept by the task, so each tick's check is a field read and an identity comparison.
            functionCompiler = Bukkit.getScheduler()
                                     .runTaskTimer(plugin, () -> compileFunctionsIfStale(plugin, server), 0L, 1L);
        }
    }

    private static void compileFunctionsIfStale(Plugin plugin, NMSDedicatedServer server) {
        try {
            Object resources = server.getDataPackResourcesHandle();
            if (resources == functionsCompiledFor && !commandsChangedSinceCompile) {
                return;
            }
            functionsCompiledFor = resources;
            commandsChangedSinceCompile = false;
            server.recompileFunctions(usesPaperCommandDispatcher() ? server.getCommandDispatcher()
                    : server.getVanillaCommandDispatcher());
        } catch (RuntimeException | LinkageError ex) {
            Objects.requireNonNull(functionCompiler)
                   .cancel();
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
            registeredHelpTopics.forEach(topic -> removeHelpTopic(Bukkit.getHelpMap(), topic, plugin.getName()));
            registeredHelpTopics.clear();
            registeredCommands.clear();
            registeredPermissions.clear();

            if (updatePlayerCommands) {
                updatePlayerCommandsLater();
                // Functions that use the commands are dropped.
                compileFunctionsWithCommands();
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
            NMSDedicatedServer server = NMSCraftServer.create(plugin.getServer())
                                                      .getServer();
            // On Paper 1.20.6+ the live dispatcher, which the command map forwards to.
            RootCommandNode root = server.getCommandDispatcher()
                                         .getBrigadier()
                                         .getRoot();
            RootCommandNode vanillaRoot = usesPaperCommandDispatcher() ? null : server.getVanillaCommandDispatcher()
                                                                                      .getBrigadier()
                                                                                      .getRoot();

            for (CommandNode<?> node : registeredCommands) {
                removeRegisteredCommand(root, knownCommands, node.getName());
                removeRegisteredCommand(root, knownCommands, namespace(plugin) + ":" + node.getName());

                // Only the Spigot dispatcher has a vanilla root of its own.
                if (vanillaRoot != null) {
                    removeCommand(vanillaRoot, node.getName());
                    removeMinecraftNamespacedCommand(root, knownCommands, node);
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
        CommandNodeChildren.replace(resourcesRoot,
                                    Collections.singletonList(label),
                                    Collections.singletonList(copy(node, label)));
    }

    /**
     * The node under another name, sharing its children.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static CommandNode<?> copy(CommandNode<?> node, String label) {
        LiteralCommandNode copy = new LiteralCommandNode(label,
                                                         node.getCommand(),
                                                         node.getRequirement(),
                                                         node.getRedirect(),
                                                         node.getRedirectModifier(),
                                                         node.isFork());
        node.getChildren()
            .forEach(child -> copy.addChild((CommandNode) child));
        return copy;
    }

    /**
     * Removes the "minecraft:&lt;name&gt;" label that CraftBukkit adds for the node when the server finishes loading.
     * Only a wrapper of this node is removed, so the label of another command stays.
     *
     * @return whether the label was removed
     */
    static boolean removeMinecraftNamespacedCommand(RootCommandNode<?> root,
                                                    Map<String, org.bukkit.command.Command> knownCommands,
                                                    CommandNode<?> node) throws Exception {
        String label = "minecraft:" + node.getName();
        org.bukkit.command.Command command = knownCommands.get(label);
        if (command == null || wrappedVanillaCommand(command) != node) {
            return false;
        }
        knownCommands.remove(label);
        removeCommand(root, label);
        return true;
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

    private static void removeCommand(CommandNode<?> commandNode, String name) {
        CommandNodeChildren.replace(commandNode, Collections.singletonList(name), Collections.emptyList());
    }
}
