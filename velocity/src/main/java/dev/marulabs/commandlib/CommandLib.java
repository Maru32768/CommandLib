package dev.marulabs.commandlib;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Entry point for registering CommandLib commands on Velocity 3.4+.
 *
 * <p>Each command and alias becomes a {@link BrigadierCommand} of the plugin. Commands can be registered at any time,
 * usually from the {@code ProxyInitializeEvent} handler. Velocity adds its commands to the command tree a backend
 * server sends, so a player who is online sees new commands the next time their server sends the tree, such as on a
 * server switch.
 *
 * <pre>{@code
 * @Plugin(id = "myplugin", name = "MyPlugin", version = "1.0")
 * public class MyPlugin {
 *     private final ProxyServer proxy;
 *
 *     @Inject
 *     public MyPlugin(ProxyServer proxy) {
 *         this.proxy = proxy;
 *     }
 *
 *     @Subscribe
 *     public void onProxyInitialize(ProxyInitializeEvent event) {
 *         CommandLib.register(this, proxy, new MyCommand());
 *     }
 * }
 * }</pre>
 */
public final class CommandLib {
    // The registrations that are not unregistered, for diagnostics().
    private static final List<CommandLib> REGISTRATIONS = new CopyOnWriteArrayList<>();

    private final String owner;
    private final String permissionPrefix;
    private final List<Command> commands;
    private final CommandManager commandManager;
    private final List<CommandMeta> registered;
    private final List<String> labels;

    private CommandLib(String owner,
                       String permissionPrefix,
                       List<Command> commands,
                       CommandManager commandManager,
                       List<CommandMeta> registered,
                       List<String> labels) {
        this.owner = owner;
        this.permissionPrefix = permissionPrefix;
        this.commands = commands;
        this.commandManager = commandManager;
        this.registered = registered;
        this.labels = labels;
    }

    /**
     * Registers the commands with the permission prefix {@code <plugin id>.command}.
     *
     * @param plugin the plugin instance, the class annotated with {@code @Plugin}
     */
    public static CommandLib register(@NotNull Object plugin,
                                      @NotNull ProxyServer proxy,
                                      @NotNull Command command,
                                      @NotNull Command... commands) {
        return register(plugin, proxy, list(command, commands));
    }

    /**
     * Registers the commands with the permission prefix {@code <plugin id>.command}.
     *
     * @param plugin the plugin instance, the class annotated with {@code @Plugin}
     */
    public static CommandLib register(@NotNull Object plugin,
                                      @NotNull ProxyServer proxy,
                                      @NotNull Collection<? extends Command> commands) {
        Objects.requireNonNull(plugin);
        String pluginId = proxy.getPluginManager()
                               .fromInstance(plugin)
                               .orElseThrow(() -> new IllegalArgumentException(plugin + " is not a Velocity plugin"))
                               .getDescription()
                               .getId();
        return register(plugin, proxy, pluginId.toLowerCase(Locale.ROOT) + ".command", commands);
    }

    public static CommandLib register(@NotNull Object plugin,
                                      @NotNull ProxyServer proxy,
                                      @NotNull String permissionPrefix,
                                      @NotNull Command command,
                                      @NotNull Command... commands) {
        return register(plugin, proxy, permissionPrefix, list(command, commands));
    }

    public static CommandLib register(@NotNull Object plugin,
                                      @NotNull ProxyServer proxy,
                                      @NotNull String permissionPrefix,
                                      @NotNull Collection<? extends Command> commands) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(proxy);
        Objects.requireNonNull(permissionPrefix);
        Objects.requireNonNull(commands);
        if (permissionPrefix.isEmpty()) {
            throw new IllegalArgumentException("permissionPrefix must not be empty");
        }
        commands.forEach(Objects::requireNonNull);
        List<Command> list = List.copyOf(commands);
        List<LiteralCommandNode<CommandSource>> nodes = new CommandNodeCreator<CommandSource, Component, CommandContext, Command>(list,
                                                                                                                                  permissionPrefix).build();
        CommandContext.proxy = proxy;
        list.forEach(c -> VelocityPermissions.addDefaults(c.permissionConfigs(permissionPrefix)));

        CommandManager commandManager = proxy.getCommandManager();
        List<CommandMeta> registered = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (LiteralCommandNode<CommandSource> node : nodes) {
            BrigadierCommand command = new BrigadierCommand(node);
            CommandMeta meta = commandManager.metaBuilder(command)
                                             .plugin(plugin)
                                             .build();
            commandManager.register(meta, command);
            registered.add(meta);
            labels.add(node.getLiteral());
        }
        String owner = proxy.getPluginManager()
                            .fromInstance(plugin)
                            .map(c -> c.getDescription()
                                       .getId())
                            .orElse(permissionPrefix);
        CommandLib instance = new CommandLib(owner,
                                             permissionPrefix,
                                             list,
                                             commandManager,
                                             registered,
                                             List.copyOf(labels));
        REGISTRATIONS.add(instance);
        return instance;
    }

    /**
     * Removes the commands this registration added.
     */
    public synchronized void unregister() {
        registered.forEach(commandManager::unregister);
        registered.clear();
        REGISTRATIONS.remove(this);
    }

    /**
     * Returns the documentation of this registration's commands with its permission prefix: Markdown and JSON of the
     * commands and of their permission nodes.
     */
    public CommandDocs docs() {
        return CommandDocs.of(permissionPrefix, commands);
    }

    /**
     * Reports the proxy and the registrations of this copy of CommandLib that are not unregistered. A plugin that
     * shades and relocates CommandLib reports only its own registrations. {@link CommandLibDiagnosticsCommand} sends it
     * to a sender.
     */
    public static CommandLibDiagnostics diagnostics() {
        List<String> platform = new ArrayList<>();
        platform.add("Artifact: velocity");
        platform.add("CommandLib package: " + CommandLib.class.getPackageName());
        ProxyServer proxy = CommandContext.proxy;
        platform.add("Proxy: " + (proxy == null ? "unknown (nothing registered yet)" : proxy.getVersion()
                                                                                            .getName()
                + " " + proxy.getVersion()
                             .getVersion()));
        platform.add("Java: " + System.getProperty("java.version"));
        platform.add("Command registration: Velocity's CommandManager (one BrigadierCommand per label)");

        List<CommandLibDiagnostics.Registration> registrations = new ArrayList<>();
        for (CommandLib instance : REGISTRATIONS) {
            registrations.add(new CommandLibDiagnostics.Registration(instance.owner,
                                                                     "BrigadierCommand",
                                                                     instance.labels,
                                                                     instance.docs()));
        }
        return new CommandLibDiagnostics(platform, registrations);
    }

    private static List<Command> list(Command command, Command... commands) {
        List<Command> list = new ArrayList<>();
        list.add(command);
        Collections.addAll(list, commands);
        return list;
    }
}
