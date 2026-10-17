package dev.marulabs.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.util.ProxyVersion;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Executes Velocity commands in tests without a running proxy. {@link CommandContext#getProxy()} returns a Mockito
 * {@link ProxyServer} ({@link #proxy()}) that knows the players and servers added with {@link #withFakePlayer} and
 * {@link #withFakeServer}, which {@code PlayerArgument} and {@code ServerArgument} look up. It returns the proxy of the
 * tester that last ran a command until that tester is closed, so async work a command queues can use it after
 * {@code execute} returns. The proxy is shared by the JVM, so testers running commands on parallel threads see each
 * other's proxy.
 * <p>
 * As on a real proxy, a permission node the sender leaves {@code UNDEFINED} is granted only if its command's
 * {@link DefaultPermission} is {@link DefaultPermission#ALL}. These defaults follow the proxy: the tester that last ran a
 * command applies its own until it is closed.
 */
public final class CommandTester implements AutoCloseable {
    private final CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();
    // Keyed by lower-case name, since the proxy looks players and servers up ignoring case.
    private final Map<String, Player> players = new LinkedHashMap<>();
    private final Map<String, RegisteredServer> servers = new LinkedHashMap<>();
    private final ProxyServer proxy = Mockito.mock(ProxyServer.class);
    private final List<Command> commands = new ArrayList<>();
    private final Map<String, DefaultPermission> permissionDefaults = new HashMap<>();
    private final String permissionPrefix;

    public static Builder builder() {
        return new Builder();
    }

    public CommandTester(@NotNull Command command, @NotNull String permissionPrefix) {
        this(builder().command(command)
                      .permissionPrefix(permissionPrefix));
    }

    public CommandTester(@NotNull Supplier<? extends Command> commandSupplier, @NotNull String permissionPrefix) {
        this(builder().command(commandSupplier)
                      .permissionPrefix(permissionPrefix));
    }

    public CommandTester(@NotNull Collection<? extends Command> commands, @NotNull String permissionPrefix) {
        this(builder().commands(commands)
                      .permissionPrefix(permissionPrefix));
    }

    private CommandTester(Builder builder) {
        this.permissionPrefix = Objects.requireNonNull(builder.permissionPrefix, "permissionPrefix");
        if (builder.commandSuppliers.isEmpty()) {
            throw new IllegalStateException("At least one command must be registered.");
        }
        builder.commandSuppliers.forEach(x -> commands.add(x.get()));
        builder.defaults.applyTo(commands);
        new CommandNodeCreator<CommandSource, Component, CommandContext, Command>(commands,
                                                                                  permissionPrefix,
                                                                                  builder.scheduler).build()
                                                                                                    .forEach(dispatcher.getRoot()::addChild);
        // The defaults CommandLib.register gives the proxy, applied to a permission no permission plugin sets.
        CommandTree.permissionConfigs(commands, permissionPrefix)
                   .forEach(x -> permissionDefaults.putIfAbsent(x.node(), x.defaultPermission()));

        Mockito.when(proxy.getVersion())
               .thenReturn(new ProxyVersion("Velocity", "CommandLib", "test"));
        Mockito.when(proxy.getAllPlayers())
               .thenAnswer(invocation -> List.copyOf(players.values()));
        Mockito.when(proxy.getPlayerCount())
               .thenAnswer(invocation -> players.size());
        Mockito.when(proxy.getPlayer(Mockito.anyString()))
               .thenAnswer(invocation -> Optional.ofNullable(players.get(key(invocation.getArgument(0)))));
        Mockito.when(proxy.getPlayer(Mockito.any(UUID.class)))
               .thenAnswer(invocation -> players.values()
                                                .stream()
                                                .filter(x -> x.getUniqueId()
                                                              .equals(invocation.getArgument(0)))
                                                .findFirst());
        Mockito.when(proxy.getAllServers())
               .thenAnswer(invocation -> List.copyOf(servers.values()));
        Mockito.when(proxy.getServer(Mockito.anyString()))
               .thenAnswer(invocation -> Optional.ofNullable(servers.get(key(invocation.getArgument(0)))));
    }

    /**
     * Executes the input as the sender and returns the command's result. A command that fails sends its failure
     * messages to the sender and returns 0.
     * An {@link AssertionError} the command throws, such as a failed assertion in an executor, is rethrown after them.
     *
     * @throws RuntimeException if the input does not match any registered command
     */
    public int execute(@NotNull String input, @NotNull FakeSender sender) {
        return execute(input, sender.asSender());
    }

    /**
     * Executes the input as a Velocity command source, like {@link #execute(String, FakeSender)}.
     *
     * @throws RuntimeException if the input does not match any registered command
     */
    public int execute(@NotNull String input, @NotNull CommandSource sender) {
        activate();
        try {
            return dispatcher.execute(input, sender);
        } catch (CommandFailure e) {
            e.sendMessages();
            e.rethrowAssertionError();
            return 0;
        } catch (CommandSyntaxException e) {
            throw new RuntimeException("Command syntax error: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the texts of the suggestions for the given partial input, waiting for them to complete.
     */
    public List<String> suggest(@NotNull String input, @NotNull FakeSender sender) {
        return suggest(input, sender.asSender());
    }

    /**
     * Returns the texts of the suggestions for the given partial input, waiting for them to complete.
     */
    public List<String> suggest(@NotNull String input, @NotNull CommandSource sender) {
        activate();
        ParseResults<CommandSource> parseResults = dispatcher.parse(input, sender);
        CompletableFuture<Suggestions> suggestions = dispatcher.getCompletionSuggestions(parseResults);
        return suggestions.join()
                          .getList()
                          .stream()
                          .map(Suggestion::getText)
                          .collect(Collectors.toList());
    }

    /**
     * Renders the registered commands as indented text: each command with its aliases and permission node, then its
     * options, argument branches and child commands.
     */
    public String commandTree() {
        return CommandTreeRenderer.render(commands, permissionPrefix);
    }

    /**
     * Compares {@link #commandTree()} with {@code src/test/resources/snapshots/<snapshotName>}. A missing snapshot
     * is written and fails the test; the environment variable {@code COMMANDLIB_UPDATE_SNAPSHOTS=true} rewrites it.
     *
     * @throws AssertionError if the tree differs from the snapshot
     */
    public void assertCommandTreeMatchesSnapshot(@NotNull String snapshotName) {
        Snapshots.assertMatches(snapshotName, commandTree());
    }

    public String permissionPrefix() {
        return permissionPrefix;
    }

    /**
     * The Mockito proxy that commands see, for additional stubbing.
     */
    public ProxyServer proxy() {
        return proxy;
    }

    /**
     * Makes the player online on the fake proxy.
     *
     * @throws IllegalArgumentException if the sender is the console
     */
    public CommandTester withFakePlayer(@NotNull FakeSender player) {
        if (!(player.asSender() instanceof Player)) {
            throw new IllegalArgumentException("sender is not a player");
        }
        return withFakePlayer((Player) player.asSender());
    }

    /**
     * Makes the player online on the fake proxy.
     */
    public CommandTester withFakePlayer(@NotNull Player player) {
        players.put(key(player.getUsername()), player);
        return this;
    }

    /**
     * Registers a Mockito server of the given name on the fake proxy and returns it.
     */
    public RegisteredServer withFakeServer(@NotNull String name) {
        RegisteredServer server = Mockito.mock(RegisteredServer.class);
        Mockito.when(server.getServerInfo())
               .thenReturn(new ServerInfo(name, InetSocketAddress.createUnresolved(name, 25565)));
        withFakeServer(server);
        return server;
    }

    /**
     * Registers the server on the fake proxy under the name of its {@link ServerInfo}.
     */
    public CommandTester withFakeServer(@NotNull RegisteredServer server) {
        servers.put(key(server.getServerInfo()
                              .getName()),
                    server);
        return this;
    }

    /**
     * Stops {@link CommandContext#getProxy()} returning this tester's proxy, unless another tester has run a command
     * since.
     */
    @Override
    public void close() {
        if (CommandContext.proxy == proxy) {
            CommandContext.proxy = null;
        }
        if (VelocityPermissions.testerDefaults == permissionDefaults) {
            VelocityPermissions.testerDefaults = Map.of();
        }
    }

    private void activate() {
        CommandContext.proxy = proxy;
        VelocityPermissions.testerDefaults = permissionDefaults;
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public static final class Builder {
        private final List<Supplier<? extends Command>> commandSuppliers = new ArrayList<>();
        private CommandScheduler scheduler = CommandScheduler.of(Runnable::run, Runnable::run);
        private CommandDefaults<CommandContext> defaults = CommandDefaults.of(CommandContext.class);
        private @Nullable String permissionPrefix;

        public Builder command(@NotNull Command command) {
            Objects.requireNonNull(command, "command");
            return command(() -> command);
        }

        public Builder command(@NotNull Supplier<? extends Command> commandSupplier) {
            commandSuppliers.add(Objects.requireNonNull(commandSupplier, "commandSupplier"));
            return this;
        }

        public Builder commands(@NotNull Collection<? extends Command> commands) {
            Objects.requireNonNull(commands, "commands");
            commands.forEach(this::command);
            return this;
        }

        public Builder permissionPrefix(@NotNull String permissionPrefix) {
            this.permissionPrefix = Objects.requireNonNull(permissionPrefix, "permissionPrefix");
            return this;
        }

        /**
         * Sets the threads of {@code executeAsync} commands. By default both executors run tasks at once, so an async
         * command completes before {@code execute} returns; pass executors that queue tasks to test what happens in
         * between.
         */
        public Builder scheduler(@NotNull CommandScheduler scheduler) {
            this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
            return this;
        }

        /**
         * Applies {@code defaults}' middlewares and context value providers to every command, as
         * {@code CommandLib.register(..., defaults, commands)} does. See {@link CommandDefaults}.
         */
        public Builder defaults(@NotNull CommandDefaults<CommandContext> defaults) {
            this.defaults = Objects.requireNonNull(defaults, "defaults");
            return this;
        }

        public CommandTester build() {
            return new CommandTester(this);
        }
    }
}
