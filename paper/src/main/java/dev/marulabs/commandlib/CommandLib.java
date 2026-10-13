package dev.marulabs.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.help.GenericCommandHelpTopic;
import org.bukkit.help.HelpMap;
import org.bukkit.help.HelpTopic;
import org.bukkit.help.HelpTopicComparator;
import org.bukkit.help.IndexHelpTopic;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final @Nullable PaperInternals PAPER = PaperInternals.find();

    // Cached from the first COMMANDS lifecycle event so that runtime calls to register() can fall back
    // to direct dispatcher manipulation instead of failing with IllegalStateException.
    private static volatile @Nullable CommandDispatcher<CommandSourceStack> serverDispatcher;
    // Bootstrap commands exist before the plugin manager does, so their permissions cannot be registered with it.
    private static final Map<String, PermissionDefault> BOOTSTRAP_PERMISSION_DEFAULTS = new ConcurrentHashMap<>();
    private static final String NULL_SENDER_CLASS = "io.papermc.paper.brigadier.NullCommandSender";
    private static final Map<Plugin, PluginRegistrationState> REGISTRATION_STATES = Collections.synchronizedMap(new WeakHashMap<>());
    private static @Nullable FunctionLoadErrorFilter functionLoadErrorFilter;

    private final Plugin plugin;
    private final PluginRegistrationState registrationState;
    private @Nullable CommandDispatcher<CommandSourceStack> dispatcher;
    private final List<String> registeredCommandNames = new ArrayList<>();
    private final List<Permission> registeredPermissions = new ArrayList<>();
    private final Map<String, Command> commandsByLabel = new HashMap<>();
    private String permissionPrefix = "";
    private boolean unregistered;
    private boolean missedCommandsEventCheckScheduled;

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
                              .toLowerCase(Locale.ROOT) + ".command",
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
        instance.registerPluginListener();
        if (functionLoadErrorFilter == null) {
            installFunctionLoadErrorFilter();
        }
        try {
            instance.doRegister(commands, permissionPrefix);
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
        Objects.requireNonNull(context);
        Objects.requireNonNull(permissionPrefix);
        Objects.requireNonNull(commands);
        if (permissionPrefix.isEmpty()) {
            throw new IllegalArgumentException("permissionPrefix must not be empty");
        }
        commands.forEach(Objects::requireNonNull);
        CommandNameValidator.validateTopLevel(commands);

        List<Command> list = List.copyOf(commands);
        list.forEach(c -> c.permissions(permissionPrefix)
                           .forEach(p -> BOOTSTRAP_PERMISSION_DEFAULTS.put(p.getName(), p.getDefault())));
        Map<String, Command> commandsByLabel = commandsByLabel(list);
        String pluginName = context.getPluginMeta()
                                   .getName();
        context.getLifecycleManager()
               .registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                   for (LiteralCommandNode<CommandSourceStack> node : buildNodes(list, permissionPrefix)) {
                       event.registrar()
                            .register(node,
                                      Objects.requireNonNull(commandsByLabel.get(node.getLiteral()))
                                             .serverHelpShortText(pluginName));
                   }
               });
    }

    /**
     * Checks a command permission, applying the default of a bootstrap command permission that nothing registered.
     */
    static boolean hasPermission(CommandSender sender, String name) {
        PermissionDefault bootstrapDefault = BOOTSTRAP_PERMISSION_DEFAULTS.get(name);
        // The server is null while functions are first compiled, and Paper's sender for compiling functions on a
        // reload has every permission, like vanilla's NULL source.
        boolean useSender = Bukkit.getServer() == null || sender.getClass()
                                                                .getName()
                                                                .equals(NULL_SENDER_CLASS);
        if (bootstrapDefault == null || useSender || sender.isPermissionSet(name) || Bukkit.getPluginManager()
                                                                                           .getPermission(name) != null) {
            return sender.hasPermission(name);
        }
        return bootstrapDefault.getValue(sender.isOp());
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

    private static List<LiteralCommandNode<CommandSourceStack>> buildNodes(Collection<? extends Command> commands,
                                                                           String permissionPrefix) {
        return new CommandNodeCreator<CommandSourceStack, net.kyori.adventure.text.Component, CommandContext, Command>(commands,
                                                                                                                       permissionPrefix).build();
    }

    private void doRegister(Collection<? extends Command> commands, String permissionPrefix) {
        doRegisterPermissions(plugin, permissionPrefix, commands);
        this.permissionPrefix = permissionPrefix;
        commandsByLabel.putAll(commandsByLabel(commands));

        PendingRegistration registration = new PendingRegistration(this, commands, permissionPrefix);
        synchronized (registrationState) {
            if (registrationState.dispatcher != null) {
                // Keep the registration so the lifecycle handler adds it again when /minecraft:reload rebuilds the
                // dispatcher.
                registrationState.registrations.add(registration);
                registerDirectly(registrationState.dispatcher, registration);
                return;
            }

            if (!registrationState.lifecycleHandlerRegistered
                    && tryRegisterLifecycleHandler(plugin, registrationState)) {
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
                throw new IllegalStateException("CommandLib.register() was called after Paper's command registration phase, "
                        + "but no dispatcher is cached yet. Ensure at least one CommandLib.register() call "
                        + "is made during plugin initialization.", registrationState.registrationFailure);
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
                        && !entry.getValue().registrations.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Paper's help topic for a registered command has no permission, so /help would list it for everyone, and its page
     * is generated. The help map is rebuilt after the COMMANDS event, so the topics are amended once it is ready.
     */
    private void amendHelpTopics() {
        HelpMap helpMap = Bukkit.getHelpMap();
        for (String label : registeredCommandNames) {
            HelpTopic topic = helpMap.getHelpTopic("/" + label);
            Command command = commandsByLabel.get(label.substring(label.indexOf(':') + 1));
            if (topic == null || command == null) {
                continue;
            }
            topic.amendCanSee(command.permissionName(permissionPrefix));
            String fullText = command.serverHelpFullText();
            if (fullText != null) {
                topic.amendTopic(null, fullText);
            }
            // At startup and on Bukkit /reload, Paper builds the plugin indexes before the COMMANDS event and keeps
            // them when it builds the help map again after it, so a plugin index made for the plugin's plugin.yml
            // commands lacks these. A direct add after the help map was built has no plugin index if the plugin had no
            // commands then.
            Collection<HelpTopic> pluginTopics = pluginIndexTopics(helpMap);
            if (pluginTopics != null) {
                pluginTopics.remove(topic);
                pluginTopics.add(topic);
            }
        }
    }

    @Nullable
    private Collection<HelpTopic> pluginIndexTopics(HelpMap helpMap) {
        String name = plugin.getName();
        HelpTopic index = helpMap.getHelpTopic(name);
        if (index != null) {
            return indexTopics(index);
        }
        Collection<HelpTopic> topics = new TreeSet<>(HelpTopicComparator.helpTopicComparatorInstance());
        // The same texts as CraftBukkit's plugin indexes.
        helpMap.addTopic(new IndexHelpTopic(name,
                                            "All commands for " + name,
                                            null,
                                            topics,
                                            "Below is a list of all " + name + " commands:"));
        return topics;
    }

    /**
     * The topics an index lists, which IndexHelpTopic keeps in a protected field without an accessor. The plugin
     * indexes of the help map hold a mutable set there.
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
              .execute(plugin, this::reloadIfCommandsEventMissed);
    }

    private void reloadIfCommandsEventMissed() {
        synchronized (registrationState) {
            if (unregistered || registrationState.dispatcher != null) {
                return;
            }
        }
        // Paper offers no public way to reach the live dispatcher outside the event, so take the /minecraft:reload
        // path: it fires the COMMANDS handlers, rebuilds the help topics, and sends the command tree to players.
        Bukkit.reloadData();
    }

    /**
     * @param runtime false when the plugin is being disabled, which cancels the tasks it schedules, so the players'
     *                command trees are updated at once
     */
    private void unregister(boolean runtime) {
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
                        replaceChildren(root, List.of(name), List.of());
                    } catch (RuntimeException e) {
                        plugin.getLogger()
                              .log(Level.WARNING, "Failed to remove command " + name, e);
                    }
                }
                removeHelpTopics();
                dispatcher = null;
                registeredCommandNames.clear();
                updateCommandTrees(runtime);
                if (runtime) {
                    recompileFunctionsLater();
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

    /**
     * Removes the commands with the given names from the root and adds the given ones, replacing those of the same
     * name. On Paper the root mirrors the vanilla dispatcher, which Paper also reads on an asynchronous pool to build
     * the players' command trees, so its children are replaced without changing the maps such a build may be
     * iterating ({@link CommandNodeChildren}), each node converted as the mirror's addChild would.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void replaceChildren(RootCommandNode<CommandSourceStack> root,
                                Collection<String> names,
                                Collection<? extends CommandNode<CommandSourceStack>> nodes) {
        if (PAPER == null || !PAPER.mirrorRoot()
                                   .isInstance(root)) {
            names.forEach(name -> removeFromNode(root, name));
            nodes.forEach(root::addChild);
            return;
        }
        try {
            CommandNode vanillaRoot = ((CommandDispatcher) PAPER.mirroredDispatcher()
                                                                .invoke(root)).getRoot();
            List<CommandNode<?>> converted = new ArrayList<>();
            for (CommandNode<CommandSourceStack> node : nodes) {
                converted.add((CommandNode<?>) PAPER.unwrapNode()
                                                    .invoke(root, node));
            }
            CommandNodeChildren.replace(vanillaRoot, names, converted);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

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
        CommandNodeChildren.replace(root, List.of(name), List.of());
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
                // Volatile, so it needs no lock.
                serverDispatcher = dispatcher;
                synchronized (state) {
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
                amendHelpTopics();
                recompileFunctionsLater();
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

    private void registerWithRegistrar(Commands registrar,
                                       CommandDispatcher<CommandSourceStack> dispatcher,
                                       PendingRegistration registration) {
        this.dispatcher = dispatcher;
        registeredCommandNames.clear();
        for (LiteralCommandNode<CommandSourceStack> node : registration.buildNodes()) {
            // Record the labels Paper actually registered. It adds the namespaced label as well, and may skip a label
            // owned by another plugin, which unregister() must then leave alone.
            registeredCommandNames.addAll(registrar.register(node,
                                                             Objects.requireNonNull(commandsByLabel.get(node.getLiteral()))
                                                                    .serverHelpShortText(plugin.getName())));
        }
    }

    private void logRegistrationFailure(RuntimeException e) {
        plugin.getLogger()
              .log(Level.SEVERE, "Failed to register commands", e);
    }

    private void registerDirectly(CommandDispatcher<CommandSourceStack> dispatcher, PendingRegistration registration) {
        addNodes(dispatcher, registration);
        showHelpTopics();
        updateCommandTrees(true);
        recompileFunctionsLater();
    }

    /**
     * Adds the nodes to the live dispatcher, so they work at once, under their labels and "&lt;plugin&gt;:&lt;label&gt;".
     * On Paper each label is a PluginCommandNode, as Paper's registrar makes them, so the command map shows a
     * PluginVanillaCommandWrapper, which names the plugin and has no minecraft.command.&lt;label&gt; permission, instead
     * of a Mojang command.
     */
    private void addNodes(CommandDispatcher<CommandSourceStack> dispatcher, PendingRegistration registration) {
        if (this.dispatcher != dispatcher) {
            this.dispatcher = dispatcher;
            registeredCommandNames.clear();
        }
        String namespace = plugin.getName()
                                 .toLowerCase(Locale.ROOT);
        List<String> labels = new ArrayList<>();
        List<LiteralCommandNode<CommandSourceStack>> labelNodes = new ArrayList<>();
        for (LiteralCommandNode<CommandSourceStack> node : registration.buildNodes()) {
            String description = Objects.requireNonNull(commandsByLabel.get(node.getLiteral()))
                                        .serverHelpShortText(plugin.getName());
            for (String label : List.of(node.getLiteral(), namespace + ":" + node.getLiteral())) {
                labels.add(label);
                labelNodes.add(labelNode(node, label, description));
            }
        }
        replaceChildren(dispatcher.getRoot(), labels, labelNodes);
        registeredCommandNames.addAll(labels);
    }

    @SuppressWarnings("unchecked")
    private LiteralCommandNode<CommandSourceStack> labelNode(LiteralCommandNode<CommandSourceStack> node,
                                                             String label,
                                                             String description) {
        if (PAPER == null) {
            return label.equals(node.getLiteral()) ? node : renamed(node, label);
        }
        try {
            return (LiteralCommandNode<CommandSourceStack>) PAPER.pluginCommandNode()
                                                                 .newInstance(label,
                                                                              plugin.getPluginMeta(),
                                                                              node,
                                                                              description);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static LiteralCommandNode<CommandSourceStack> renamed(LiteralCommandNode<CommandSourceStack> node, String label) {
        LiteralCommandNode<CommandSourceStack> copy = new LiteralCommandNode<>(label,
                                                                               node.getCommand(),
                                                                               node.getRequirement(),
                                                                               node.getRedirect(),
                                                                               node.getRedirectModifier(),
                                                                               node.isFork());
        node.getChildren()
            .forEach(copy::addChild);
        return copy;
    }

    /**
     * Paper builds the help map from the command map only when the server loads and on a data reload, so the labels of
     * a direct add get their topics here.
     */
    private void showHelpTopics() {
        HelpMap helpMap = Bukkit.getHelpMap();
        for (String label : registeredCommandNames) {
            if (helpMap.getHelpTopic("/" + label) != null) {
                continue;
            }
            org.bukkit.command.Command wrapper = Bukkit.getCommandMap()
                                                       .getCommand(label);
            if (wrapper != null) {
                helpMap.addTopic(new GenericCommandHelpTopic(wrapper));
            }
        }
        amendHelpTopics();
    }

    private void removeHelpTopics() {
        HelpMap helpMap = Bukkit.getHelpMap();
        HelpTopic index = helpMap.getHelpTopic(plugin.getName());
        Collection<HelpTopic> pluginTopics = indexTopics(index);
        for (String label : registeredCommandNames) {
            HelpTopic topic = helpMap.getHelpTopic("/" + label);
            if (topic == null) {
                continue;
            }
            // CraftBukkit's getHelpTopics() is a live view of its topics.
            helpMap.getHelpTopics()
                   .remove(topic);
            if (pluginTopics != null) {
                pluginTopics.remove(topic);
            }
        }
        if (pluginTopics != null && pluginTopics.isEmpty()) {
            helpMap.getHelpTopics()
                   .remove(index);
        }
    }

    /**
     * Sends the players their command trees after a change to the live dispatcher, on the next tick so that the changes
     * of one tick share one update, or at once while the plugin is being disabled. Paper builds the trees on an
     * asynchronous pool while the main thread goes on, which a change of the root's children maps would break
     * (ConcurrentModificationException), so the changes replace those maps instead ({@link #replaceChildren}).
     */
    private void updateCommandTrees(boolean later) {
        if (!later) {
            if (!Bukkit.isStopping()) {
                sendCommandTrees();
            }
            return;
        }
        if (registrationState.commandTreeUpdateScheduled) {
            return;
        }
        registrationState.commandTreeUpdateScheduled = true;
        Bukkit.getGlobalRegionScheduler()
              .execute(plugin, () -> {
                  registrationState.commandTreeUpdateScheduled = false;
                  sendCommandTrees();
              });
    }

    /**
     * Compiles the data pack functions again on the next tick, once for the changes of a tick, after the server compiled
     * them without the commands ({@link ServerFunctions}). Bukkit and global region tasks run before the server runs
     * functions in a tick, so the #load functions of a data reload run once, with the commands.
     */
    private void recompileFunctionsLater() {
        if (registrationState.functionRecompileScheduled || !plugin.isEnabled()) {
            return;
        }
        registrationState.functionRecompileScheduled = true;
        Bukkit.getGlobalRegionScheduler()
              .execute(plugin, () -> {
                  registrationState.functionRecompileScheduled = false;
                  ServerFunctions.recompileFunctions(plugin.getLogger());
              });
    }

    private static void sendCommandTrees() {
        Bukkit.getOnlinePlayers()
              .forEach(Player::updateCommands);
    }

    /**
     * Paper's internal classes for changing its live dispatcher as its registrar does.
     */
    private record PaperInternals(Class<?> mirrorRoot, Method mirroredDispatcher, Method unwrapNode,
            Constructor<?> pluginCommandNode) {
        @Nullable
        static PaperInternals find() {
            try {
                Class<?> mirrorRoot = Class.forName("io.papermc.paper.command.brigadier.ApiMirrorRootNode");
                Method unwrapNode = mirrorRoot.getDeclaredMethod("unwrapNode", CommandNode.class);
                unwrapNode.setAccessible(true);
                return new PaperInternals(mirrorRoot,
                                          mirrorRoot.getMethod("getDispatcher"),
                                          unwrapNode,
                                          Class.forName("io.papermc.paper.command.brigadier.PluginCommandNode")
                                               .getConstructor(String.class,
                                                               PluginMeta.class,
                                                               LiteralCommandNode.class,
                                                               String.class));
            } catch (ReflectiveOperationException | RuntimeException e) {
                // Not a Paper server, as in tests, or Paper changed these internals: nodes are then added as plain
                // nodes and the root's own methods change it.
                return null;
            }
        }
    }

    private static final class PluginRegistrationState {
        private boolean lifecycleHandlerRegistered;
        // Main thread only: the changes of one tick share one update of the players' command trees.
        private boolean commandTreeUpdateScheduled;
        private boolean functionRecompileScheduled;
        private @Nullable IllegalStateException registrationFailure;
        private @Nullable CommandDispatcher<CommandSourceStack> dispatcher;
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
            return CommandLib.buildNodes(commands, permissionPrefix);
        }
    }
}
