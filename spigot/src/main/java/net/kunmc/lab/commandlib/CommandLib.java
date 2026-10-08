package net.kunmc.lab.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.kunmc.lab.commandlib.util.bukkit.BukkitUtil;
import net.kunmc.lab.commandlib.util.bukkit.MinecraftVersion;
import net.kunmc.lab.commandlib.util.nms.command.NMSCommandDispatcher;
import net.kunmc.lab.commandlib.util.nms.command.NMSVanillaCommandWrapper;
import net.kunmc.lab.commandlib.util.nms.server.NMSCraftServer;
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
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public final class CommandLib implements Listener {
    private final Plugin plugin;
    private final Collection<? extends Command> commands;
    private final String permissionPrefix;
    private final List<CommandNode<?>> registeredCommands = new ArrayList<>();
    private final List<Permission> registeredPermissions = new ArrayList<>();

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
                                    .toLowerCase() + ".command");
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
        CommandMap commandMap = ((CommandMap) NMSCraftServer.create()
                                                            .getValue("commandMap"));
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
                NMSCommandDispatcher dispatcher = NMSCraftServer.create(plugin.getServer())
                                                                .getServer()
                                                                .getCommandDispatcher();
                RootCommandNode root = dispatcher.getBrigadier()
                                                 .getRoot();
                for (CommandNode x : registeredCommands) {
                    removeRegisteredCommand(root, knownCommands, x.getName());
                    removeExecuteRunCommand(root, x.getName());

                    root.addChild(x);
                    BukkitCommand wrapper = NMSVanillaCommandWrapper.create()
                                                                    .createInstance(dispatcher, x);
                    wrapper.setPermission(null);
                    knownCommands.put(x.getName(), wrapper);

                    CommandNode executeRunRoot = executeRunRedirectRoot(root);
                    if (executeRunRoot != null) {
                        removeCommand(executeRunRoot, x.getName());
                        executeRunRoot.addChild(x);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        updatePlayerCommandsLater();
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
            if (usePaperCommandDispatcher) {
                root = paperDispatcherRoot(knownCommands);
            } else {
                root = NMSCraftServer.create(plugin.getServer())
                                     .getServer()
                                     .getCommandDispatcher()
                                     .getBrigadier()
                                     .getRoot();
            }

            for (String s : registeredCommands.stream()
                                              .map(CommandNode::getName)
                                              .collect(Collectors.toList())) {
                removeRegisteredCommand(root, knownCommands, s);

                if (!usePaperCommandDispatcher) {
                    removeExecuteRunCommand(root, s);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    private static void removeExecuteRunCommand(RootCommandNode<?> root, String name) throws Exception {
        CommandNode<?> executeRunRoot = executeRunRedirectRoot(root);
        if (executeRunRoot == null) {
            return;
        }

        removeCommand(executeRunRoot, name);
    }

    private static CommandNode<?> executeRunRedirectRoot(RootCommandNode<?> root) {
        CommandNode<?> execute = root.getChild("execute");
        if (execute == null) {
            return null;
        }

        CommandNode<?> run = execute.getChild("run");
        if (run == null || run.getRedirect() == null) {
            return null;
        }

        return run.getRedirect();
    }
}
