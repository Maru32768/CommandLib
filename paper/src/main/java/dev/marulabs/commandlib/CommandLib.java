package dev.marulabs.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.marulabs.commandlib.PluginRegistrationState.Mode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
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
 *
 * <p>Paper compiles data pack functions before it fires the commands event for plugins, at startup and on every data
 * reload, so CommandLib compiles them again on the next tick for functions to use the commands. Commands registered from
 * a Paper plugin bootstrapper with {@link #bootstrap} exist before the server compiles the functions.
 */
@SuppressWarnings("UnstableApiUsage")
public final class CommandLib implements Listener {
    // Cached from the first COMMANDS lifecycle event so that runtime calls to register() can fall back
    // to direct dispatcher manipulation instead of failing with IllegalStateException.
    private static volatile @Nullable CommandDispatcher<CommandSourceStack> serverDispatcher;
    private static final Map<Plugin, PluginRegistrationState> REGISTRATION_STATES = Collections.synchronizedMap(new WeakHashMap<>());
    private static @Nullable FunctionLoadErrorFilter functionLoadErrorFilter;
    // The bootstrap registrations, which last for the server's lifetime.
    private static final List<BootstrapRegistration> BOOTSTRAP_REGISTRATIONS = new CopyOnWriteArrayList<>();

    private final Plugin plugin;
    private final PluginRegistrationState registrationState;
    private final CommandSet commands;
    // Guarded by registrationState.
    private State state = State.PENDING;
    private final List<Permission> registeredPermissions = new ArrayList<>();
    private boolean missedCommandsEventCheckScheduled;

    /**
     * Where this registration's commands are: in no dispatcher yet, in one under the labels they got, or removed for
     * good.
     */
    private sealed interface State {
        State PENDING = new Pending();
        State UNREGISTERED = new Unregistered();

        record Pending() implements State {
        }

        /**
         * @param labels the labels the commands got there, which unregister() removes, leaving those of others alone
         * @param nodes  the node that held each label then, which tells whether another registration took it since
         */
        record Live(CommandDispatcher<CommandSourceStack> dispatcher, List<String> labels,
                Map<String, Object> nodes) implements State {
            static Live of(CommandDispatcher<CommandSourceStack> dispatcher, List<String> labels) {
                Map<String, Object> nodes = new HashMap<>();
                labels.forEach(x -> nodes.put(x, PaperDispatcher.child(dispatcher.getRoot(), x)));
                return new Live(dispatcher, List.copyOf(labels), nodes);
            }

            boolean owns(String label) {
                return PaperDispatcher.child(dispatcher.getRoot(), label) == nodes.get(label);
            }
        }

        record Unregistered() implements State {
        }
    }

    private CommandLib(Plugin plugin, PluginRegistrationState registrationState, CommandSet commands) {
        this.plugin = plugin;
        this.registrationState = registrationState;
        this.commands = commands;
    }

    public static CommandLib register(@NotNull Plugin plugin, @NotNull Command command, @NotNull Command... commands) {
        List<Command> list = new ArrayList<>();
        list.add(command);
        Collections.addAll(list, commands);
        return register(plugin, list);
    }

    public static CommandLib register(@NotNull Plugin plugin, @NotNull Collection<? extends Command> commands) {
        return register(plugin, CommandDefaults.of(CommandContext.class), commands);
    }

    /**
     * Same as {@link #register(Plugin, Collection)}, with the middlewares and context value providers of
     * {@code defaults} applied to every command. See {@link CommandDefaults}.
     */
    public static CommandLib register(@NotNull Plugin plugin,
                                      @NotNull CommandDefaults<CommandContext> defaults,
                                      @NotNull Collection<? extends Command> commands) {
        return register(plugin,
                        plugin.getName()
                              .toLowerCase(Locale.ROOT) + ".command",
                        defaults,
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
        return register(plugin, permissionPrefix, CommandDefaults.of(CommandContext.class), commands);
    }

    /**
     * Same as {@link #register(Plugin, String, Collection)}, with the middlewares and context value providers of
     * {@code defaults} applied to every command. See {@link CommandDefaults}.
     */
    public static CommandLib register(@NotNull Plugin plugin,
                                      @NotNull String permissionPrefix,
                                      @NotNull CommandDefaults<CommandContext> defaults,
                                      @NotNull Collection<? extends Command> commands) {
        Objects.requireNonNull(plugin);
        requireGlobalThread("Commands must be registered");
        defaults.applyTo(commands);
        CommandSet commandSet = CommandSet.of(permissionPrefix,
                                              commands,
                                              FoliaCommandScheduler.forServer(() -> plugin));

        CommandLib instance = new CommandLib(plugin,
                                             REGISTRATION_STATES.computeIfAbsent(plugin,
                                                                                 ignored -> new PluginRegistrationState()),
                                             commandSet);
        instance.registerPluginListener();
        if (functionLoadErrorFilter == null) {
            installFunctionLoadErrorFilter();
        }
        try {
            instance.doRegister();
        } catch (RuntimeException e) {
            // Leave nothing behind for a registration that never took effect, so the permissions and the disable
            // listener do not leak and a later reload does not try to add it again.
            instance.unregister(false);
            throw e;
        }
        if (plugin.isEnabled()) {
            instance.scheduleMissedCommandsEventCheck();
        }
        return instance;
    }

    /**
     * Registers commands from a Paper plugin bootstrapper ({@code PluginBootstrap#bootstrap}), so they exist before
     * data pack functions are compiled and functions can use them.
     *
     * <p>The commands stay registered for the lifetime of the server and are added again on every
     * {@code /minecraft:reload}; there is no way to unregister them. Paper refuses {@code /reload} while a bootstrapper
     * handles the commands event. The server does not exist yet during bootstrap, so the command permissions are not
     * registered with Bukkit's plugin manager; CommandLib applies their defaults itself unless a permission plugin sets
     * the permission or it is declared in {@code paper-plugin.yml}.
     *
     * <pre>{@code
     * public class MyBootstrap implements PluginBootstrap {
     *     @Override
     *     public void bootstrap(BootstrapContext context) {
     *         CommandLib.bootstrap(context, new MyCommand());
     *     }
     * }
     * }</pre>
     */
    public static void bootstrap(@NotNull BootstrapContext context,
                                 @NotNull Command command,
                                 @NotNull Command... commands) {
        List<Command> list = new ArrayList<>();
        list.add(command);
        Collections.addAll(list, commands);
        bootstrap(context,
                  context.getPluginMeta()
                         .getName()
                         .toLowerCase(Locale.ROOT) + ".command",
                  list);
    }

    /**
     * Same as {@link #bootstrap(BootstrapContext, Command, Command...)} with a custom permission prefix.
     */
    public static void bootstrap(@NotNull BootstrapContext context,
                                 @NotNull String permissionPrefix,
                                 @NotNull Collection<? extends Command> commands) {
        bootstrap(context, permissionPrefix, CommandDefaults.of(CommandContext.class), commands);
    }

    /**
     * Same as {@link #bootstrap(BootstrapContext, String, Collection)}, with the middlewares and context value
     * providers of {@code defaults} applied to every command. See {@link CommandDefaults}.
     */
    public static void bootstrap(@NotNull BootstrapContext context,
                                 @NotNull String permissionPrefix,
                                 @NotNull CommandDefaults<CommandContext> defaults,
                                 @NotNull Collection<? extends Command> commands) {
        Objects.requireNonNull(context);
        defaults.applyTo(commands);
        String pluginName = context.getPluginMeta()
                                   .getName();
        CommandSet commandSet = CommandSet.of(permissionPrefix,
                                              commands,
                                              FoliaCommandScheduler.forServer(() -> Bukkit.getPluginManager()
                                                                                          .getPlugin(pluginName)));

        CommandPermissions.addBootstrapDefaults(commandSet);
        BootstrapRegistration registration = new BootstrapRegistration(pluginName, commandSet);
        BOOTSTRAP_REGISTRATIONS.add(registration);
        context.getLifecycleManager()
               .registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                   List<String> labels = new ArrayList<>();
                   for (LiteralCommandNode<CommandSourceStack> node : commandSet.buildNodes()) {
                       labels.addAll(event.registrar()
                                          .register(node, commandSet.description(node, pluginName)));
                   }
                   registration.labels = List.copyOf(labels);
               });
    }

    /**
     * Returns the documentation of this registration's commands with its permission prefix: Markdown and JSON of the
     * commands and of the permission nodes registered for them.
     */
    public CommandDocs docs() {
        return CommandDocs.of(commands.permissionPrefix(), commands.commands());
    }

    /**
     * Reports the platform and the registrations of this copy of CommandLib, including bootstrap ones, that are not
     * unregistered: the server and Minecraft version, how each registration reaches Paper's dispatcher, its labels,
     * and its commands and permissions. A plugin that shades and relocates CommandLib reports only its own
     * registrations. {@link CommandLibDiagnosticsCommand} sends it to a sender.
     */
    public static CommandLibDiagnostics diagnostics() {
        List<String> platform = new ArrayList<>();
        platform.add("Artifact: paper");
        platform.add("CommandLib package: " + CommandLib.class.getPackageName());
        platform.add("Server: " + Bukkit.getName() + " " + Bukkit.getVersion());
        platform.add("Minecraft: " + Bukkit.getMinecraftVersion());
        platform.add("Java: " + System.getProperty("java.version"));
        platform.add("Command registration: Paper's Commands API (LifecycleEvents.COMMANDS)");
        platform.add("Live dispatcher: "
                + (serverDispatcher == null ? "not seen yet (no COMMANDS event so far)" : "seen in a COMMANDS event"));

        List<CommandLibDiagnostics.Registration> registrations = new ArrayList<>();
        for (BootstrapRegistration registration : BOOTSTRAP_REGISTRATIONS) {
            registrations.add(new CommandLibDiagnostics.Registration(registration.pluginName,
                                                                     "bootstrap COMMANDS lifecycle handler",
                                                                     registration.labels,
                                                                     CommandDocs.of(registration.commands.permissionPrefix(),
                                                                                    registration.commands.commands())));
        }
        List<Map.Entry<Plugin, PluginRegistrationState>> entries;
        synchronized (REGISTRATION_STATES) {
            entries = new ArrayList<>(REGISTRATION_STATES.entrySet());
        }
        for (Map.Entry<Plugin, PluginRegistrationState> entry : entries) {
            synchronized (entry.getValue()) {
                String path = registrationPath(entry.getValue().mode);
                for (CommandLib instance : entry.getValue().instances) {
                    registrations.add(new CommandLibDiagnostics.Registration(entry.getKey()
                                                                                  .getName(),
                                                                             path,
                                                                             instance.liveLabels(),
                                                                             instance.docs()));
                }
            }
        }
        return new CommandLibDiagnostics(platform, registrations);
    }

    private static String registrationPath(Mode mode) {
        return switch (mode) {
            case Mode.Unhandled ignored -> "no COMMANDS lifecycle handler";
            case Mode.Handled handled when handled.dispatcher() == null ->
                "COMMANDS lifecycle handler, waiting for the event";
            case Mode.Handled ignored -> "COMMANDS lifecycle handler";
        };
    }

    /**
     * A registration from {@link #bootstrap}: the commands, and the labels Paper gave them in the last COMMANDS event.
     */
    private static final class BootstrapRegistration {
        private final String pluginName;
        private final CommandSet commands;
        private volatile List<String> labels = List.of();

        private BootstrapRegistration(String pluginName, CommandSet commands) {
            this.pluginName = pluginName;
            this.commands = commands;
        }
    }

    private void doRegister() {
        for (Permission permission : commands.permissions()) {
            CommandPermissions.add(permission);
            registeredPermissions.add(permission);
        }

        synchronized (registrationState) {
            switch (registrationState.mode) {
                case Mode.Unhandled ignored -> {
                    IllegalStateException failure = registerLifecycleHandler(plugin, registrationState);
                    if (failure == null) {
                        // The handler adds it when the event fires.
                        registrationState.mode = new Mode.Handled(null);
                        registrationState.instances.add(this);
                        return;
                    }
                    // The initialization window has passed for this plugin, so Paper refuses the handler that would add
                    // the commands again on every data reload.
                    throw new IllegalStateException("CommandLib.register() was first called for " + plugin.getName()
                            + " after its initialization. Make the first CommandLib.register() call during the "
                            + "plugin's initialization (constructor, onLoad or onEnable); later ones are kept across "
                            + "data reloads.", failure);
                }
                case Mode.Handled handled when handled.dispatcher() == null -> {
                    // The handler adds it when the event fires.
                    registrationState.instances.add(this);
                }
                default -> {
                    // Kept so that /minecraft:reload adds it again to the dispatcher it builds.
                    registrationState.instances.add(this);
                    registerDirectly(Objects.requireNonNull(registrationState.mode.dispatcher()));
                }
            }
        }
    }

    /**
     * Sends the command tree to the player again, so the commands and branches they see follow a change of their
     * permissions without a rejoin. The server sends the tree on join and when the player's operator status changes,
     * but not when a permission changes, such as through a {@code PermissionAttachment}. Must be called on the main
     * server thread, or on Folia on any region or global region thread; Folia sends the tree on the player's own thread.
     *
     * @throws IllegalStateException if called from another thread
     */
    public static void refresh(@NotNull Player player) {
        Objects.requireNonNull(player);
        requirePrimaryThread("Command trees must be refreshed");
        player.updateCommands();
    }

    /**
     * Same as {@link #refresh(Player)} for every online player, on the same threads.
     *
     * @throws IllegalStateException if called from another thread
     */
    public static void refreshAll() {
        requirePrimaryThread("Command trees must be refreshed");
        Bukkit.getOnlinePlayers()
              .forEach(Player::updateCommands);
    }

    // The dispatcher and the help and permission maps are main-thread only; permission plugins and config reloads
    // often run on other threads. On Folia every region thread is a primary thread.
    private static void requirePrimaryThread(String action) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(action + " on the main server thread (on Folia, a region or global region "
                    + "thread)");
        }
    }

    // On Folia register and unregister change state shared by every region, so a region thread is refused too. The
    // server's own thread, which enables and disables plugins, is not a region thread.
    private static void requireGlobalThread(String action) {
        if (!Bukkit.isPrimaryThread() || FoliaCommandScheduler.onRegionThread()) {
            throw new IllegalStateException(action + " on the main server thread (on Folia, the global region thread)");
        }
    }

    /**
     * Removes all commands and permissions registered by this instance.
     * Must be called on the main server thread, on Folia the global region thread.
     */
    public void unregister() {
        requireGlobalThread("Commands must be unregistered");
        unregister(true);
    }

    private void onPluginDisable(PluginDisableEvent event) {
        if (!event.getPlugin()
                  .equals(plugin)) {
            return;
        }

        // Every instance of the plugin is removed first, so the functions are compiled once without all its commands.
        List<CommandLib> instances;
        synchronized (registrationState) {
            instances = new ArrayList<>(registrationState.instances);
        }
        instances.add(this);
        boolean removed = false;
        for (CommandLib instance : instances) {
            removed |= instance.unregister(false);
        }
        // Compiled functions hold the nodes they use, so they would run the disabled plugin's commands until the next
        // data reload. The plugin's tasks are cancelled, so they are compiled now.
        if (removed && !Bukkit.isStopping()) {
            FunctionCompilationMarker.changed();
            ServerFunctions.recompileFunctions(plugin.getLogger());
        }
        // Shaded into this plugin, this copy compiles functions only for it.
        if (functionLoadErrorFilter != null) {
            functionLoadErrorFilter.uninstall();
            functionLoadErrorFilter = null;
        }
    }

    /**
     * Hides the errors of the server's compilation of functions that use the commands, since CommandLib compiles them
     * again after every data reload ({@link FunctionLoadErrorFilter}).
     */
    private static void installFunctionLoadErrorFilter() {
        try {
            functionLoadErrorFilter = FunctionLoadErrorFilter.install(Bukkit.getServer()
                                                                            .getClass()
                                                                            .getClassLoader(),
                                                                      () -> ServerFunctions.available()
                                                                              && hasEnabledRegistrations());
        } catch (LinkageError | RuntimeException ignored) {
            // The server does not log through this Log4j, so there is nothing to filter.
        }
    }

    private static boolean hasEnabledRegistrations() {
        List<Map.Entry<Plugin, PluginRegistrationState>> entries;
        synchronized (REGISTRATION_STATES) {
            entries = new ArrayList<>(REGISTRATION_STATES.entrySet());
        }
        for (Map.Entry<Plugin, PluginRegistrationState> entry : entries) {
            synchronized (entry.getValue()) {
                if (entry.getKey()
                         .isEnabled()
                        && !entry.getValue().instances.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin()
                 .equals(plugin)) {
            scheduleMissedCommandsEventCheck();
        }
    }

    /**
     * A plugin enabled after startup (e.g. by PlugMan) can still install its COMMANDS handler, but the event already
     * fired, so the handler would only run on the next reload. At startup the event fires before the first tick, so a
     * handler that has not run by then has missed it.
     */
    private void scheduleMissedCommandsEventCheck() {
        // register() during onEnable() schedules the check, and the PluginEnableEvent that follows would do it again.
        if (missedCommandsEventCheckScheduled) {
            return;
        }
        missedCommandsEventCheckScheduled = true;
        Bukkit.getGlobalRegionScheduler()
              .execute(plugin, this::addIfCommandsEventMissed);
    }

    /**
     * Adds the plugin's registrations to the live dispatcher as a registration after initialization does, which also
     * adds the help topics and compiles the functions again. The plugin's handler adds them on the next reload.
     */
    private void addIfCommandsEventMissed() {
        synchronized (registrationState) {
            if (state == State.UNREGISTERED || !(registrationState.mode instanceof Mode.Handled handled)
                    || handled.dispatcher() != null) {
                return;
            }
            CommandDispatcher<CommandSourceStack> dispatcher = PaperDispatcher.live(serverDispatcher);
            if (dispatcher != null) {
                registrationState.mode = new Mode.Handled(dispatcher);
                for (CommandLib instance : registrationState.instances) {
                    try {
                        instance.registerDirectly(dispatcher);
                    } catch (RuntimeException e) {
                        instance.logRegistrationFailure(e);
                    }
                }
                return;
            }
        }
        if (FoliaCommandScheduler.ON_FOLIA) {
            // Folia has no data reload, so the commands come with the next server start.
            plugin.getLogger()
                  .warning("The commands of " + plugin.getName()
                          + " missed Paper's COMMANDS event and cannot be added on Folia until the server restarts");
            return;
        }
        // Without Paper's dispatcher, take the /minecraft:reload path: it fires the COMMANDS handlers, rebuilds the help
        // topics, and sends the command tree to players.
        Bukkit.reloadData();
    }

    /**
     * @param runtime false when the plugin is being disabled, which cancels the tasks it schedules, so the players'
     *                command trees are updated at once
     * @return whether commands were removed from the live dispatcher
     */
    private boolean unregister(boolean runtime) {
        State previous;
        synchronized (registrationState) {
            previous = state;
            if (previous == State.UNREGISTERED) {
                return false;
            }
            state = State.UNREGISTERED;
            registrationState.instances.remove(this);
        }

        try {
            if (previous instanceof State.Live live) {
                List<String> removed = new ArrayList<>();
                for (String label : live.labels()) {
                    // Keep going so that one failure does not leave the remaining commands and the permissions
                    // registered, since unregister() cannot be retried.
                    try {
                        // A later registration of the same label took it, and keeps it.
                        if (!live.owns(label)) {
                            continue;
                        }
                        removed.add(label);
                        PaperDispatcher.replaceChildren(live.dispatcher()
                                                            .getRoot(),
                                                        List.of(label),
                                                        List.of());
                    } catch (RuntimeException e) {
                        plugin.getLogger()
                              .log(Level.WARNING, "Failed to remove command " + label, e);
                    }
                }
                HelpTopics.remove(plugin, removed);
                registrationState.updateCommandTrees(plugin, runtime);
                if (runtime) {
                    registrationState.recompileFunctionsLater(plugin);
                }
            }

            registeredPermissions.forEach(CommandPermissions::remove);
            registeredPermissions.clear();
        } finally {
            HandlerList.unregisterAll(this);
        }
        return previous instanceof State.Live;
    }

    /**
     * @return null when the handler is installed, or why Paper refused it
     */
    @Nullable
    private static IllegalStateException registerLifecycleHandler(Plugin plugin,
                                                                  PluginRegistrationState registrationState) {
        try {
            plugin.getLifecycleManager()
                  .registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                      Commands registrar = event.registrar();
                      CommandDispatcher<CommandSourceStack> dispatcher = registrar.getDispatcher();
                      // Volatile, so it needs no lock.
                      serverDispatcher = dispatcher;
                      synchronized (registrationState) {
                          registrationState.mode = new Mode.Handled(dispatcher);

                          // The event fires at startup and again after /minecraft:reload with a fresh dispatcher, so
                          // every active registration is added each time instead of only the pending ones.
                          for (CommandLib instance : registrationState.instances) {
                              // Paper only logs an exception thrown from this handler, so one failing registration
                              // must not keep the others from being added.
                              try {
                                  instance.registerWithRegistrar(registrar, dispatcher);
                              } catch (RuntimeException e) {
                                  instance.logRegistrationFailure(e);
                              }
                          }
                      }
                  });
            return null;
        } catch (IllegalStateException e) {
            return e;
        }
    }

    private void registerPluginListener() {
        // PluginManager#registerEvents rejects plugins that are not enabled yet, which is the case when register() is
        // called from the constructor or onLoad(), and a later COMMANDS lifecycle event may also fire before
        // onEnable(). Registering with the handler lists directly works in every phase. PluginDisableEvent is fired
        // before the plugin is marked disabled, so the listener still receives it.
        RegisteredListener listener = new RegisteredListener(this, (ignored, event) -> {
            if (event instanceof PluginDisableEvent) {
                onPluginDisable((PluginDisableEvent) event);
            } else if (event instanceof PluginEnableEvent) {
                onPluginEnable((PluginEnableEvent) event);
            } else {
                // ServerLoadEvent after startup or Bukkit /reload, ServerResourcesReloadedEvent after a data reload.
                HelpTopics.amend(plugin, commands, liveLabels());
                registrationState.recompileFunctionsLater(plugin);
            }
        }, EventPriority.NORMAL, plugin, false);
        PluginDisableEvent.getHandlerList()
                          .register(listener);
        PluginEnableEvent.getHandlerList()
                         .register(listener);
        ServerLoadEvent.getHandlerList()
                       .register(listener);
        ServerResourcesReloadedEvent.getHandlerList()
                                    .register(listener);
    }

    /**
     * The labels the commands have in the live dispatcher, none while they are in none.
     */
    private List<String> liveLabels() {
        synchronized (registrationState) {
            return state instanceof State.Live live ? live.labels() : List.of();
        }
    }

    private void registerWithRegistrar(Commands registrar, CommandDispatcher<CommandSourceStack> dispatcher) {
        List<String> labels = new ArrayList<>();
        try {
            for (LiteralCommandNode<CommandSourceStack> node : commands.buildNodes()) {
                // Record the labels Paper actually registered. It adds the namespaced label as well, and may skip a
                // label owned by another plugin, which unregister() must then leave alone.
                labels.addAll(registrar.register(node, commands.description(node, plugin.getName())));
            }
        } finally {
            // Also when a node fails, so unregister() removes the labels registered before it.
            state = State.Live.of(dispatcher, labels);
        }
    }

    private void logRegistrationFailure(RuntimeException e) {
        plugin.getLogger()
              .log(Level.SEVERE, "Failed to register commands", e);
    }

    private void registerDirectly(CommandDispatcher<CommandSourceStack> dispatcher) {
        List<String> labels = addNodes(dispatcher);
        HelpTopics.show(plugin, commands, labels);
        registrationState.updateCommandTrees(plugin, true);
        registrationState.recompileFunctionsLater(plugin);
    }

    /**
     * Adds the nodes to the live dispatcher, so they work at once, under their labels and "&lt;plugin&gt;:&lt;label&gt;"
     * ({@link PaperDispatcher#labelNode}).
     *
     * @return the labels added
     */
    private List<String> addNodes(CommandDispatcher<CommandSourceStack> dispatcher) {
        String namespace = plugin.getName()
                                 .toLowerCase(Locale.ROOT);
        List<String> labels = new ArrayList<>();
        List<LiteralCommandNode<CommandSourceStack>> labelNodes = new ArrayList<>();
        for (LiteralCommandNode<CommandSourceStack> node : commands.buildNodes()) {
            String description = commands.description(node, plugin.getName());
            for (String label : List.of(node.getLiteral(), namespace + ":" + node.getLiteral())) {
                labels.add(label);
                labelNodes.add(PaperDispatcher.labelNode(plugin, node, label, description));
            }
        }
        PaperDispatcher.replaceChildren(dispatcher.getRoot(), labels, labelNodes);
        List<String> added = List.copyOf(labels);
        state = State.Live.of(dispatcher, added);
        return added;
    }
}
