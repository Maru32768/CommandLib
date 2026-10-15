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
    private final CommandManager commandManager;
    private final List<CommandMeta> registered;

    private CommandLib(CommandManager commandManager, List<CommandMeta> registered) {
        this.commandManager = commandManager;
        this.registered = registered;
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
        for (LiteralCommandNode<CommandSource> node : nodes) {
            BrigadierCommand command = new BrigadierCommand(node);
            CommandMeta meta = commandManager.metaBuilder(command)
                                             .plugin(plugin)
                                             .build();
            commandManager.register(meta, command);
            registered.add(meta);
        }
        return new CommandLib(commandManager, registered);
    }

    /**
     * Removes the commands this registration added.
     */
    public synchronized void unregister() {
        registered.forEach(commandManager::unregister);
        registered.clear();
    }

    private static List<Command> list(Command command, Command... commands) {
        List<Command> list = new ArrayList<>();
        list.add(command);
        Collections.addAll(list, commands);
        return list;
    }
}
