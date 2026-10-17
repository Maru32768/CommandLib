package dev.marulabs.commandlib;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import dev.marulabs.commandlib.util.CurrentServer;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.Mockito;
import org.mockito.MockedStatic;
//? if >=1.19.3 {
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
//?}
//? if >=1.20.5 {
/*import net.minecraft.core.RegistrationInfo;
*///?} elif >=1.19.3 {
import net.minecraft.resources.RegistryDataLoader;

import java.util.stream.Stream;
//?}

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
//? if neoforge {
/*import net.neoforged.neoforge.common.util.flag.FeatureFlagLoader;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
*///?} elif forge && >=1.17 {
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.server.ServerLifecycleHooks;
//?} elif forge
/*import net.minecraftforge.fml.server.ServerLifecycleHooks;*/
//? if >=1.19.3 {
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.WorldData;
//?}
//? if <1.18
/*import net.minecraftforge.server.permission.PermissionAPI;*/
//? if neoforge {
/*import net.minecraft.commands.SharedSuggestionProvider;
import net.neoforged.neoforge.common.CommonHooks;
*///?} elif forge && >=1.19 {
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraftforge.common.ForgeHooks;
//?}

/**
 * Runs CommandLib Forge / NeoForge / Fabric commands without a Minecraft server.
 *
 * <p>Minecraft's registries are bootstrapped once per JVM, so vanilla argument types such as items, blocks, and
 * effects parse for real. The server's {@code registryAccess()} holds the built-in registries (before 1.19.3 its built-in
 * data-driven ones, with the others in {@code Registry.REGISTRY}), and from 1.19.3 also the data-driven registries such
 * as biomes and enchantments, built from vanilla's registry bootstrap code. Tags are not loaded. The server is a
 * Mockito mock whose player list contains the fake players added with {@link #withPlayer(FakeSender)} and the player
 * executing each command, so player and entity arguments resolve those players by name, and whose levels are those
 * added with {@link #withLevel(ServerLevel)} and, unless one is added, a mock overworld. Each command source is in the
 * overworld. The server treats every thread as its own, so it runs tasks at once, and its {@code getCommands()}
 * dispatcher holds the tester's commands, so {@code CommandLib.refresh} works. {@link #withServer(Consumer)} stubs more of it. Permission
 * nodes are answered by the executing fake player; the console passes every check, as on a server.</p>
 *
 * <p>The server and the static stubs, such as Forge's {@code ServerLifecycleHooks.getCurrentServer()}, live while a
 * command executes, and again while a task it handed to the {@link Builder#scheduler(CommandScheduler) scheduler}
 * runs later, on whichever thread runs it.</p>
 */
public final class CommandTester implements AutoCloseable {
    // The scope open on this thread, which tasks handed to the scheduler from it reopen.
    private static final ThreadLocal<Scope> CURRENT_SCOPE = new ThreadLocal<>();

    private final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
    private final Map<String, FakeSender> players = new LinkedHashMap<>();
    private final Map<ResourceKey<Level>, ServerLevel> levels = new LinkedHashMap<>();
    private final List<Consumer<MinecraftServer>> serverSetups = new ArrayList<>();
    private final List<Command> commands = new ArrayList<>();
    private final String permissionPrefix;
    @Nullable
    private ServerLevel defaultOverworld;

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

        MinecraftBootstrap.ensureBootstrapped();
        commands.addAll(builder.createCommands());
        new CommandNodeCreator<>(commands, permissionPrefix, inScope(builder.scheduler)).build()
                                                                                        .forEach(dispatcher.getRoot()::addChild);
    }

    // MockedStatic applies only on the thread that opened it and until execute returns, so a task the command hands to
    // the scheduler reopens the command's scope on the thread that runs it.
    private CommandScheduler inScope(CommandScheduler scheduler) {
        return new CommandScheduler() {
            @Override
            public Executor sync(CommonCommandContext<?, ?> ctx) {
                return inScope(scheduler.sync(ctx));
            }

            @Override
            public Executor async(CommonCommandContext<?, ?> ctx) {
                return inScope(scheduler.async(ctx));
            }
        };
    }

    private Executor inScope(Executor executor) {
        return task -> {
            Scope scope = CURRENT_SCOPE.get();
            executor.execute(scope == null ? task : () -> {
                if (CURRENT_SCOPE.get() != null) {
                    task.run();
                    return;
                }
                try (Scope reopened = new Scope(scope)) {
                    task.run();
                }
            });
        };
    }

    /**
     * Executes the given command input as the specified fake sender and returns the command's result, which
     * {@code /execute store result} would store. A command that fails sends its failure messages to the sender, as the
     * server does, and returns 0.
     * An {@link AssertionError} the command throws, such as a failed assertion in an executor, is rethrown after them.
     *
     * @throws RuntimeException if the input does not match any registered command
     */
    public int execute(@NotNull String input, @NotNull FakeSender sender) {
        try (Scope scope = new Scope(sender)) {
            try {
                return CommandFailure.replayed(() -> dispatcher.execute(input, sender.createSource(scope.server)));
            } catch (CommandFailure e) {
                // Sent inside the scope, which the source's messages may need.
                e.sendMessages();
                e.rethrowAssertionError();
                return 0;
            }
        } catch (CommandSyntaxException e) {
            throw new RuntimeException("Command syntax error: " + e.getMessage(), e);
        }
    }

    /**
     * Returns Brigadier suggestions for the given partial input.
     */
    public CompletableFuture<Suggestions> suggestions(@NotNull String input, @NotNull FakeSender sender) {
        try (Scope scope = new Scope(sender)) {
            CommandSourceStack source = sender.createSource(scope.server);
            // Suggestion providers run synchronously for CommandLib arguments, so join while the scope is open.
            Suggestions suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(input, source))
                                                .join();
            return CompletableFuture.completedFuture(suggestions);
        }
    }

    /**
     * Returns the texts of the suggestions for the given partial input.
     */
    public List<String> suggest(@NotNull String input, @NotNull FakeSender sender) {
        return suggestions(input, sender).join()
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

    /**
     * Adds a fake player to the server's player list, so player and entity arguments can find it by name. It replaces
     * an added player with the same name, ignoring case.
     */
    public CommandTester withPlayer(@NotNull FakeSender player) {
        if (!player.isPlayer()) {
            throw new IllegalArgumentException("sender is not a player");
        }
        players.put(key(player.getName()), player);
        return this;
    }

    boolean hasPlayer(String name) {
        return players.containsKey(key(name));
    }

    /**
     * Adds a level to the server under its {@link ServerLevel#dimension()}, so {@code WorldArgument} parses and
     * suggests it. The level is usually a Mockito mock. From 1.19.3, a mock whose {@code enabledFeatures()} answers
     * null is stubbed to answer the default feature flags, which selectors check entity types against.
     */
    public CommandTester withLevel(@NotNull ServerLevel level) {
        levels.put(Objects.requireNonNull(level.dimension(), "level.dimension()"), level);
        //? if >=1.19.3 {
        stubFeatures(level);
        //?}
        return this;
    }

    /**
     * Runs {@code setup} on the server mock of every command execution and suggestion request, after the tester has
     * stubbed it. Stub what an argument reads from the server with it, such as {@code getScoreboard()},
     * {@code getAdvancements()} or {@code getRecipeManager()}.
     */
    public CommandTester withServer(@NotNull Consumer<MinecraftServer> setup) {
        serverSetups.add(Objects.requireNonNull(setup, "setup"));
        return this;
    }

    /**
     * Does nothing. Mocks only live while a command, or a task it handed to the scheduler, runs; this exists so tests
     * can use try-with-resources like the other CommandLib testing artifacts.
     */
    @Override
    public void close() {
    }

    /**
     * Keeps the server mock and the static stubs for one command execution, or for a task it handed to the scheduler.
     */
    private final class Scope implements AutoCloseable {
        private final MinecraftServer server;
        private final Map<String, FakeSender> onlinePlayers;
        private final List<MockedStatic<?>> staticMocks = new ArrayList<>();

        private Scope(FakeSender sender) {
            onlinePlayers = new LinkedHashMap<>(players);
            if (sender.isPlayer()) {
                // The executing player is online, even when an added player has its name.
                onlinePlayers.put(key(sender.getName()), sender);
            }
            server = createServer(onlinePlayers, serverLevels(), dispatcher);
            serverSetups.forEach(setup -> setup.accept(server));
            open();
        }

        // Reopens the stubs of a command's scope on the thread running a task the command handed to the scheduler.
        private Scope(Scope command) {
            onlinePlayers = command.onlinePlayers;
            server = command.server;
            open();
        }

        private void open() {
            try {
                MockedStatic<CurrentServer> currentServer = Mockito.mockStatic(CurrentServer.class);
                staticMocks.add(currentServer);
                currentServer.when(CurrentServer::get)
                             .thenReturn(server);
                //? if forge || neoforge {
                MockedStatic<ServerLifecycleHooks> lifecycleHooks = Mockito.mockStatic(ServerLifecycleHooks.class);
                staticMocks.add(lifecycleHooks);
                lifecycleHooks.when(ServerLifecycleHooks::getCurrentServer)
                              .thenReturn(server);
                //?}

                //? if >=1.18 {
                MockedStatic<CommandLib> commandLib = Mockito.mockStatic(CommandLib.class, Mockito.CALLS_REAL_METHODS);
                staticMocks.add(commandLib);
                commandLib.when(() -> CommandLib.hasPermission(Mockito.any(),
                                                               Mockito.any(ServerPlayer.class),
                                                               Mockito.anyString()))
                          .thenAnswer(invocation -> hasPermission(onlinePlayers,
                                                                  invocation.getArgument(1),
                                                                  invocation.getArgument(2)));
                //?} else {
                /*MockedStatic<PermissionAPI> permissionApi = Mockito.mockStatic(PermissionAPI.class,
                                                                               Mockito.CALLS_REAL_METHODS);
                staticMocks.add(permissionApi);
                permissionApi.when(() -> PermissionAPI.hasPermission(Mockito.any(net.minecraft.world.entity.player.Player.class),
                                                                     Mockito.anyString()))
                             .thenAnswer(invocation -> hasPermission(onlinePlayers,
                                                                     invocation.getArgument(0),
                                                                     invocation.getArgument(1)));
                *///?}
                   // Forge and NeoForge ask their permission API, which has no handler here, whether a player may use
                   // entity selectors (in a message, for example). Answer as their default does: permission level 2.
                   //? if neoforge {
                   /*MockedStatic<CommonHooks> hooks = Mockito.mockStatic(CommonHooks.class, Mockito.CALLS_REAL_METHODS);
                   staticMocks.add(hooks);
                   hooks.when(() -> CommonHooks.canUseEntitySelectors(Mockito.any()))
                        .thenAnswer(invocation -> invocation.<SharedSuggestionProvider>getArgument(0)
                                                            .hasPermission(2));
                   *///?} elif forge && >=1.19 {
                MockedStatic<ForgeHooks> hooks = Mockito.mockStatic(ForgeHooks.class, Mockito.CALLS_REAL_METHODS);
                staticMocks.add(hooks);
                hooks.when(() -> ForgeHooks.canUseEntitySelectors(Mockito.any()))
                     .thenAnswer(invocation -> invocation.<SharedSuggestionProvider>getArgument(0)
                                                         .hasPermission(2));
                //?}
            } catch (RuntimeException | Error e) {
                close();
                throw e;
            }
            CURRENT_SCOPE.set(this);
        }

        @Override
        public void close() {
            for (int i = staticMocks.size() - 1; i >= 0; i--) {
                staticMocks.get(i)
                           .close();
            }
            staticMocks.clear();
            if (CURRENT_SCOPE.get() == this) {
                CURRENT_SCOPE.remove();
            }
        }
    }

    // A server always has an overworld, the level of every command source.
    private Map<ResourceKey<Level>, ServerLevel> serverLevels() {
        if (levels.containsKey(Level.OVERWORLD)) {
            return levels;
        }
        if (defaultOverworld == null) {
            defaultOverworld = Mockito.mock(ServerLevel.class);
            Mockito.when(defaultOverworld.dimension())
                   .thenReturn(Level.OVERWORLD);
            //? if >=1.19.3 {
            stubFeatures(defaultOverworld);
            //?}
        }
        Map<ResourceKey<Level>, ServerLevel> serverLevels = new LinkedHashMap<>();
        serverLevels.put(Level.OVERWORLD, defaultOverworld);
        serverLevels.putAll(levels);
        return serverLevels;
    }

    //? if >=1.19.3 {
    // Selectors check entity types against the source's features, which some versions read from its level.
    private static void stubFeatures(ServerLevel level) {
        if (Mockito.mockingDetails(level)
                   .isMock()
                && level.enabledFeatures() == null) {
            Mockito.lenient()
                   .when(level.enabledFeatures())
                   .thenReturn(FeatureFlags.DEFAULT_FLAGS);
        }
    }
    //?}

    private static boolean hasPermission(Map<String, FakeSender> onlinePlayers, Object player, String node) {
        return onlinePlayers.values()
                            .stream()
                            .filter(sender -> sender.asPlayer()
                                                    .orElse(null) == player)
                            .findFirst()
                            .map(sender -> sender.hasPermission(node))
                            .orElse(false);
    }

    private static MinecraftServer createServer(Map<String, FakeSender> onlinePlayers,
                                                Map<ResourceKey<Level>, ServerLevel> levels,
                                                CommandDispatcher<CommandSourceStack> dispatcher) {
        List<ServerPlayer> playerList = new ArrayList<>();
        onlinePlayers.values()
                     .forEach(sender -> sender.asPlayer()
                                              .ifPresent(playerList::add));

        PlayerList players = Mockito.mock(PlayerList.class);
        Mockito.when(players.getPlayers())
               .thenReturn(Collections.unmodifiableList(playerList));
        Mockito.when(players.getPlayerByName(Mockito.anyString()))
               .thenAnswer(invocation -> findPlayer(onlinePlayers,
                                                    invocation.getArgument(0)).flatMap(FakeSender::asPlayer)
                                                                              .orElse(null));
        Mockito.when(players.getPlayer(Mockito.any(java.util.UUID.class)))
               .thenAnswer(invocation -> playerList.stream()
                                                   .filter(player -> player.getUUID()
                                                                           .equals(invocation.getArgument(0)))
                                                   .findFirst()
                                                   .orElse(null));
        String[] playerNames = onlinePlayers.values()
                                            .stream()
                                            .map(FakeSender::getName)
                                            .toArray(String[]::new);
        Mockito.when(players.getPlayerNamesArray())
               .thenReturn(playerNames);

        GameProfileCache profileCache = Mockito.mock(GameProfileCache.class);
        Mockito.when(profileCache.get(Mockito.anyString()))
               .thenAnswer(invocation -> {
                   Optional<GameProfile> profile = findPlayer(onlinePlayers,
                                                              invocation.getArgument(0)).flatMap(FakeSender::asPlayer)
                                                                                        .map(ServerPlayer::getGameProfile);
                   //? if >=1.17 {
                   return profile;
                   //?} else
                   /*return profile.orElse(null);*/
               });

        MinecraftServer server = Mockito.mock(MinecraftServer.class);
        Mockito.when(server.getPlayerList())
               .thenReturn(players);
        Mockito.when(server.getProfileCache())
               .thenReturn(profileCache);
        Mockito.when(server.getPlayerNames())
               .thenReturn(playerNames);
        Map<ResourceKey<Level>, ServerLevel> levelMap = Map.copyOf(levels);
        Mockito.when(server.getLevel(Mockito.any()))
               .thenAnswer(invocation -> levelMap.get(invocation.<ResourceKey<Level>>getArgument(0)));
        Mockito.when(server.levelKeys())
               .thenReturn(levelMap.keySet());
        Mockito.when(server.getAllLevels())
               .thenReturn(levelMap.values());
        Mockito.when(server.overworld())
               .thenReturn(levelMap.get(Level.OVERWORLD));
        // Tasks such as CommandLib.refresh run at once, as on the server thread.
        Mockito.when(server.isSameThread())
               .thenReturn(true);
        Commands commands = Mockito.mock(Commands.class);
        Mockito.when(commands.getDispatcher())
               .thenReturn(dispatcher);
        Mockito.when(server.getCommands())
               .thenReturn(commands);
        playerList.forEach(player -> Mockito.when(player.getServer())
                                            .thenReturn(server));
        //? if >=1.19.3 {
        WorldData worldData = Mockito.mock(WorldData.class);
        Mockito.when(worldData.enabledFeatures())
               .thenReturn(FeatureFlags.DEFAULT_FLAGS);
        Mockito.when(server.getWorldData())
               .thenReturn(worldData);
        //?}
        //? if >=1.19.3 {
        RegistryAccess.Frozen registryAccess = MinecraftBootstrap.registryAccess();
        //?} elif >=1.18.2 {
        /*RegistryAccess.Frozen registryAccess = RegistryAccess.BUILTIN.get();
        *///?} else
        /*RegistryAccess registryAccess = RegistryAccess.builtin();*/
        Mockito.when(server.registryAccess())
               .thenReturn(registryAccess);
        return server;
    }

    private static Optional<FakeSender> findPlayer(Map<String, FakeSender> onlinePlayers, String name) {
        return Optional.ofNullable(onlinePlayers.get(key(name)));
    }

    // Player names are unique ignoring case, as the server looks them up.
    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public static final class Builder {
        private final List<Supplier<? extends Command>> commandSuppliers = new ArrayList<>();
        private CommandScheduler scheduler = CommandScheduler.of(Runnable::run, Runnable::run);
        private CommandDefaults<CommandContext> defaults = CommandDefaults.of(CommandContext.class);
        @Nullable
        private String permissionPrefix;

        private Builder() {
        }

        public Builder command(@NotNull Command command) {
            Objects.requireNonNull(command, "command");
            commandSuppliers.add(() -> command);
            return this;
        }

        public Builder command(@NotNull Supplier<? extends Command> commandSupplier) {
            commandSuppliers.add(Objects.requireNonNull(commandSupplier, "commandSupplier"));
            return this;
        }

        public Builder commands(@NotNull Collection<? extends Command> commands) {
            Objects.requireNonNull(commands, "commands")
                   .forEach(this::command);
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

        private List<Command> createCommands() {
            List<Command> commands = new ArrayList<>();
            for (Supplier<? extends Command> supplier : commandSuppliers) {
                commands.add(Objects.requireNonNull(supplier.get(), "command supplier returned null"));
            }
            defaults.applyTo(commands);
            return commands;
        }
    }

    static final class MinecraftBootstrap {
        private static boolean bootstrapped;
        //? if >=1.19.3 {
        @Nullable
        private static RegistryAccess.Frozen registryAccess;

        // The server's registries, including data-driven ones such as biomes and enchantments, built from vanilla's
        // registry bootstrap code instead of loading the vanilla data pack.
        private static synchronized RegistryAccess.Frozen registryAccess() {
            if (registryAccess != null) {
                return registryAccess;
            }
            List<Registry<?>> registries = new ArrayList<>();
            BuiltInRegistries.REGISTRY.forEach(registries::add);
            HolderLookup.Provider vanilla = VanillaRegistries.createLookup();
            //? if >=1.21.2 {
            /*vanilla.listRegistryKeys()
                   .filter(key -> !BuiltInRegistries.REGISTRY.containsKey(key.location()))
                   .forEach(key -> registries.add(copy(vanilla.lookupOrThrow(key))));
            *///?} elif >=1.20.5 {
            /*vanilla.listRegistries()
                   .filter(key -> !BuiltInRegistries.REGISTRY.containsKey(key.location()))
                   .forEach(key -> registries.add(copy(vanilla.lookupOrThrow(key))));
            *///?} else {
               // The lookup cannot list its registries before 1.20.5, so ask it for each data-driven one.
            Stream.concat(RegistryDataLoader.WORLDGEN_REGISTRIES.stream(),
                          RegistryDataLoader.DIMENSION_REGISTRIES.stream())
                  .map(RegistryDataLoader.RegistryData::key)
                  .filter(key -> !BuiltInRegistries.REGISTRY.containsKey(key.location()))
                  .forEach(key -> vanilla.lookup(key)
                                         .ifPresent(lookup -> registries.add(copy(lookup))));
            //?}
            registryAccess = new RegistryAccess.ImmutableRegistryAccess(registries).freeze();
            return registryAccess;
        }

        @SuppressWarnings("unchecked")
        private static <T> Registry<T> copy(HolderLookup.RegistryLookup<T> lookup) {
            MappedRegistry<T> registry = new MappedRegistry<>((ResourceKey<? extends Registry<T>>) lookup.key(),
                                                              Lifecycle.stable());
            //? if >=1.20.5 {
            /*lookup.listElements()
                  .forEach(holder -> registry.register(holder.key(), holder.value(), RegistrationInfo.BUILT_IN));
            *///?} else {
            lookup.listElements()
                  .forEach(holder -> registry.register(holder.key(), holder.value(), Lifecycle.stable()));
            //?}
            return registry.freeze();
        }
        //?}

        static synchronized void ensureBootstrapped() {
            if (bootstrapped) {
                return;
            }
            //? if >=1.18
            SharedConstants.tryDetectVersion();
            //? if neoforge {
            /*// NeoForge's FeatureFlags patch loads modded feature flags from FML's mod list, which only exists in a
            // real NeoForge launch. Tests only use vanilla feature flags, so skip it.
            try (MockedStatic<FeatureFlagLoader> ignored = Mockito.mockStatic(FeatureFlagLoader.class)) {
                Bootstrap.bootStrap();
            }
            *///?} elif forge && >=1.17 {
               // Forge's Bootstrap patch initializes networking, whose event classes need FML's class transformer.
               // Commands do not use networking, so skip it outside a real Forge launch.
            try (MockedStatic<NetworkHooks> ignored = Mockito.mockStatic(NetworkHooks.class)) {
                Bootstrap.bootStrap();
            }
            //?} else
            /*Bootstrap.bootStrap();*/
            bootstrapped = true;
        }
    }
}
