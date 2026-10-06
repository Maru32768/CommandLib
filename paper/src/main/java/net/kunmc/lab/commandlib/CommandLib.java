package net.kunmc.lab.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.*;
import java.util.logging.Level;

/**
 * Entry point for registering CommandLib commands on Paper 1.21.0+.
 *
 * <p>Call {@link #register} during plugin initialization, such as from your plugin's
 * constructor, {@code onLoad()}, or {@code onEnable()}, so the lifecycle event handler
 * is installed before the server processes commands.
 *
 * <pre>{@code
 * public class MyPlugin extends JavaPlugin {
 *     public MyPlugin() {
 *         CommandLib.register(this, new MyCommand());
 *     }
 * }
 * }</pre>
 */
@SuppressWarnings("UnstableApiUsage")
public final class CommandLib implements Listener {
    private static final VarHandle CHILDREN;
    private static final VarHandle LITERALS;
    private static final VarHandle ARGUMENTS;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(CommandNode.class, MethodHandles.lookup());
            CHILDREN = lookup.findVarHandle(CommandNode.class, "children", Map.class);
            LITERALS = lookup.findVarHandle(CommandNode.class, "literals", Map.class);
            ARGUMENTS = lookup.findVarHandle(CommandNode.class, "arguments", Map.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // Cached from the first COMMANDS lifecycle event so that runtime calls to register() can fall back
    // to direct dispatcher manipulation instead of failing with IllegalStateException.
    private static volatile CommandDispatcher<CommandSourceStack> serverDispatcher;
    private static final Map<Plugin, PluginRegistrationState> REGISTRATION_STATES = Collections.synchronizedMap(new WeakHashMap<>());

    private final Plugin plugin;
    private final PluginRegistrationState registrationState;
    private CommandDispatcher<CommandSourceStack> dispatcher;
    private final List<String> registeredCommandNames = new ArrayList<>();
    private final List<Permission> registeredPermissions = new ArrayList<>();
    private boolean unregistered;

    private CommandLib(Plugin plugin, PluginRegistrationState registrationState) {
        this.plugin = plugin;
        this.registrationState = registrationState;
    }

    public static CommandLib register(@NotNull Plugin plugin, @NotNull Command command, @NotNull Command... commands) {
        List<Command> list = new ArrayList<>();
        list.add(command);
        Collections.addAll(list, commands);
        return register(plugin, list);
    }

    public static CommandLib register(@NotNull Plugin plugin, @NotNull Collection<? extends Command> commands) {
        return register(plugin,
                        plugin.getName()
                              .toLowerCase() + ".command",
                        commands);
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
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(permissionPrefix);
        Objects.requireNonNull(commands);
        if (permissionPrefix.isEmpty()) {
            throw new IllegalArgumentException("permissionPrefix must not be empty");
        }
        commands.forEach(Objects::requireNonNull);
        CommandNameValidator.validateTopLevel(commands);

        CommandLib instance = new CommandLib(plugin,
                                             REGISTRATION_STATES.computeIfAbsent(plugin,
                                                                                 ignored -> new PluginRegistrationState()));
        instance.registerDisableListener();
        try {
            instance.doRegister(commands, permissionPrefix);
        } catch (RuntimeException e) {
            // Leave nothing behind for a registration that never took effect, so the permissions and the disable
            // listener do not leak and a later reload does not try to add it again.
            instance.unregister(false);
            throw e;
        }
        return instance;
    }

    private void doRegister(Collection<? extends Command> commands, String permissionPrefix) {
        doRegisterPermissions(plugin, permissionPrefix, commands);

        PendingRegistration registration = new PendingRegistration(this, commands, permissionPrefix);
        synchronized (registrationState) {
            if (registrationState.dispatcher != null) {
                // Keep the registration so the lifecycle handler adds it again when /minecraft:reload rebuilds the
                // dispatcher.
                registrationState.registrations.add(registration);
                registerDirectly(registrationState.dispatcher, registration);
                return;
            }

            if (!registrationState.lifecycleHandlerRegistered && tryRegisterLifecycleHandler(plugin,
                                                                                             registrationState)) {
                registrationState.lifecycleHandlerRegistered = true;
            }

            if (registrationState.lifecycleHandlerRegistered) {
                registrationState.registrations.add(registration);
                return;
            }

            // The initialization window has already passed for this plugin, so the lifecycle
            // handler can no longer be installed. Fall back to direct dispatcher manipulation.
            CommandDispatcher<CommandSourceStack> dispatcher = serverDispatcher;
            if (dispatcher == null) {
                throw new IllegalStateException(
                        "CommandLib.register() was called after Paper's command registration phase, " + "but no dispatcher is cached yet. Ensure at least one CommandLib.register() call " + "is made during plugin initialization.",
                        registrationState.registrationFailure);
            }
            registrationState.dispatcher = dispatcher;
            // Kept so that a COMMANDS event fired for another plugin re-adds it after /minecraft:reload.
            registrationState.registrations.add(registration);
            registerDirectly(dispatcher, registration);
        }
    }

    /**
     * Removes all commands and permissions registered by this instance.
     * Must be called on the main server thread.
     */
    public void unregister() {
        unregister(true);
    }

    private void onPluginDisable(PluginDisableEvent event) {
        if (!event.getPlugin()
                  .equals(plugin)) {
            return;
        }

        unregister(false);
    }

    private void unregister(boolean updatePlayerCommands) {
        if (unregistered) {
            return;
        }
        unregistered = true;

        synchronized (registrationState) {
            registrationState.registrations.removeIf(x -> x.instance == this);
        }

        try {
            if (dispatcher != null) {
                RootCommandNode<CommandSourceStack> root = dispatcher.getRoot();
                for (String name : registeredCommandNames) {
                    // Keep going so that one failure does not leave the remaining commands and the permissions
                    // registered, since unregister() cannot be retried.
                    try {
                        removeFromNode(root, name);
                    } catch (RuntimeException e) {
                        plugin.getLogger()
                              .log(Level.WARNING, "Failed to remove command " + name, e);
                    }
                }
                dispatcher = null;
                registeredCommandNames.clear();

                if (updatePlayerCommands) {
                    Bukkit.getOnlinePlayers()
                          .forEach(Player::updateCommands);
                }
            }

            for (Permission permission : new ArrayList<>(registeredPermissions)) {
                permission.setDefault(PermissionDefault.FALSE);
                Bukkit.getPluginManager()
                      .recalculatePermissionDefaults(permission);
                Bukkit.getPluginManager()
                      .removePermission(permission);
            }
            registeredPermissions.clear();
        } finally {
            HandlerList.unregisterAll(this);
        }
    }

    @SuppressWarnings("unchecked")
    private static void removeFromNode(RootCommandNode<CommandSourceStack> root, String name) {
        // Paper's dispatcher root mirrors the vanilla dispatcher and overrides its own removeCommand(String) to
        // remove the node there as well. Editing the root's maps directly would leave the command executable.
        try {
            root.getClass()
                .getMethod("removeCommand", String.class)
                .invoke(root, name);
            return;
        } catch (NoSuchMethodException ignored) {
            // Plain Brigadier (e.g. in tests) has no removeCommand.
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to remove command " + name, e);
        }
        ((Map<String, ?>) CHILDREN.get(root)).remove(name);
        ((Map<String, ?>) LITERALS.get(root)).remove(name);
        ((Map<String, ?>) ARGUMENTS.get(root)).remove(name);
    }

    private void doRegisterPermissions(Plugin plugin, String permissionPrefix, Collection<? extends Command> commands) {
        commands.stream()
                .flatMap(c -> c.permissions(permissionPrefix)
                               .stream())
                .forEach(permission -> {
                    removePermissionIfPresent(plugin, permission.getName());
                    plugin.getServer()
                          .getPluginManager()
                          .addPermission(permission);
                    registeredPermissions.add(permission);
                });
    }

    private static void removePermissionIfPresent(Plugin plugin, String name) {
        Permission existing = plugin.getServer()
                                    .getPluginManager()
                                    .getPermission(name);
        if (existing != null) {
            existing.setDefault(PermissionDefault.FALSE);
            plugin.getServer()
                  .getPluginManager()
                  .recalculatePermissionDefaults(existing);
            plugin.getServer()
                  .getPluginManager()
                  .removePermission(name);
        }
    }

    private static boolean tryRegisterLifecycleHandler(Plugin plugin, PluginRegistrationState state) {
        LifecycleEventManager<Plugin> lifecycleManager = plugin.getLifecycleManager();
        try {
            lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                Commands registrar = event.registrar();
                CommandDispatcher<CommandSourceStack> dispatcher = registrar.getDispatcher();
                synchronized (state) {
                    serverDispatcher = dispatcher;
                    state.dispatcher = dispatcher;

                    // The event fires at startup and again after /minecraft:reload with a fresh dispatcher, so every
                    // active registration is added each time instead of only the pending ones.
                    for (PendingRegistration registration : state.registrations) {
                        // Paper only logs an exception thrown from this handler, so one failing registration must
                        // not keep the others from being added.
                        try {
                            registration.instance.registerWithRegistrar(registrar, dispatcher, registration);
                        } catch (RuntimeException e) {
                            registration.instance.logRegistrationFailure(e);
                        }
                    }
                }
                reregisterFallbackStates(state, dispatcher);
            });
            return true;
        } catch (IllegalStateException e) {
            state.registrationFailure = e;
            return false;
        }
    }

    /**
     * Plugins that registered after their initialization window have no COMMANDS handler of their own, so their
     * commands are re-added to the fresh dispatcher whenever another plugin's handler observes a reload.
     */
    private static void reregisterFallbackStates(PluginRegistrationState owner,
                                                 CommandDispatcher<CommandSourceStack> dispatcher) {
        List<PluginRegistrationState> states;
        synchronized (REGISTRATION_STATES) {
            states = new ArrayList<>(REGISTRATION_STATES.values());
        }
        for (PluginRegistrationState state : states) {
            if (state == owner) {
                continue;
            }
            synchronized (state) {
                if (state.lifecycleHandlerRegistered || state.dispatcher == null || state.dispatcher == dispatcher) {
                    continue;
                }
                state.dispatcher = dispatcher;
                for (PendingRegistration registration : state.registrations) {
                    try {
                        registration.instance.addNodes(dispatcher, registration);
                    } catch (RuntimeException e) {
                        registration.instance.logRegistrationFailure(e);
                    }
                }
            }
        }
    }

    private void registerDisableListener() {
        // PluginManager#registerEvents rejects plugins that are not enabled yet, which is the case when register() is
        // called from the constructor or onLoad(), and a later COMMANDS lifecycle event may also fire before
        // onEnable(). Registering with the handler list directly works in every phase. PluginDisableEvent is fired
        // before the plugin is marked disabled, so the listener still receives it.
        PluginDisableEvent.getHandlerList()
                          .register(new RegisteredListener(this, (listener, event) -> {
                              if (event instanceof PluginDisableEvent) {
                                  onPluginDisable((PluginDisableEvent) event);
                              }
                          }, EventPriority.NORMAL, plugin, false));
    }

    private void registerWithRegistrar(Commands registrar,
                                       CommandDispatcher<CommandSourceStack> dispatcher,
                                       PendingRegistration registration) {
        this.dispatcher = dispatcher;
        registeredCommandNames.clear();
        for (LiteralCommandNode<CommandSourceStack> node : registration.buildNodes()) {
            // Record the labels Paper actually registered. It adds the namespaced label as well, and may skip a label
            // owned by another plugin, which unregister() must then leave alone.
            registeredCommandNames.addAll(registrar.register(node, ""));
        }
    }

    private void logRegistrationFailure(RuntimeException e) {
        plugin.getLogger()
              .log(Level.SEVERE, "Failed to register commands", e);
    }

    private void registerDirectly(CommandDispatcher<CommandSourceStack> dispatcher, PendingRegistration registration) {
        addNodes(dispatcher, registration);
        Bukkit.getOnlinePlayers()
              .forEach(Player::updateCommands);
    }

    private void addNodes(CommandDispatcher<CommandSourceStack> dispatcher, PendingRegistration registration) {
        if (this.dispatcher != dispatcher) {
            this.dispatcher = dispatcher;
            registeredCommandNames.clear();
        }
        for (LiteralCommandNode<CommandSourceStack> node : registration.buildNodes()) {
            dispatcher.getRoot()
                      .addChild(node);
            registeredCommandNames.add(node.getLiteral());
        }
    }

    private static final class PluginRegistrationState {
        private boolean lifecycleHandlerRegistered;
        private IllegalStateException registrationFailure;
        private CommandDispatcher<CommandSourceStack> dispatcher;
        private final List<PendingRegistration> registrations = new ArrayList<>();
    }

    private static final class PendingRegistration {
        private final CommandLib instance;
        private final Collection<? extends Command> commands;
        private final String permissionPrefix;

        private PendingRegistration(CommandLib instance,
                                    Collection<? extends Command> commands,
                                    String permissionPrefix) {
            this.instance = instance;
            this.commands = List.copyOf(commands);
            this.permissionPrefix = permissionPrefix;
        }

        private List<LiteralCommandNode<CommandSourceStack>> buildNodes() {
            return new CommandNodeCreator<>(commands, permissionPrefix).build();
        }
    }
}
