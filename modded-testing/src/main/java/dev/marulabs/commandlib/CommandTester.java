package dev.marulabs.commandlib;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import dev.marulabs.commandlib.util.CurrentServer;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.Mockito;
import org.mockito.MockedStatic;

//? if >=1.20.5 {
/*import com.mojang.serialization.Lifecycle;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
*///?}

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
//? if neoforge {
/*import net.neoforged.neoforge.common.util.flag.FeatureFlagLoader;
*///?} elif forge && >=1.17 {
import net.minecraftforge.network.NetworkHooks;
//?}
//? if >=1.18 {
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.WorldData;
//?} else
/*import net.minecraftforge.server.permission.PermissionAPI;*/
//? if neoforge {
/*import net.minecraft.commands.SharedSuggestionProvider;
import net.neoforged.neoforge.common.CommonHooks;
*///?} elif forge && >=1.18 {
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraftforge.common.ForgeHooks;
//?}

/**
 * Runs CommandLib Forge / NeoForge / Fabric commands without a Minecraft server.
 *
 * <p>Minecraft's registries are bootstrapped once per JVM, so vanilla argument types such as items, blocks, and
 * effects parse for real. On 1.20.5+ the server's {@code registryAccess()} also contains the data-driven registries
 * such as enchantments, built from vanilla's registry bootstrap code. Tags are not loaded. The server is a Mockito
 * mock whose player list contains the fake players added with {@link #withPlayer(FakeSender)} and the player executing
 * each command, so player and entity arguments resolve those players by name. Permission nodes are answered by the executing {@link FakeSender}.</p>
 */
public final class CommandTester implements AutoCloseable {
    private final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
    private final Map<String, FakeSender> players = new LinkedHashMap<>();
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

        MinecraftBootstrap.ensureBootstrapped();
        new CommandNodeCreator<>(builder.createCommands(), permissionPrefix, builder.scheduler).build()
                                                                                               .forEach(dispatcher.getRoot()::addChild);
    }

    /**
     * Executes the given command input as the specified fake sender and returns the command's result, which
     * {@code /execute store result} would store. A command that fails sends its failure messages to the sender, as the
     * server does, and returns 0.
     *
     * @throws RuntimeException if the input does not match any registered command
     */
    public int execute(@NotNull String input, @NotNull FakeSender sender) {
        try (Scope scope = new Scope(sender)) {
            try {
                return dispatcher.execute(input, sender.createSource(scope.server));
            } catch (CommandFailure e) {
                // Sent inside the scope, which the source's messages may need.
                e.sendMessages();
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
     * Adds a fake player to the server's player list, so player and entity arguments can find it by name.
     */
    public CommandTester withPlayer(@NotNull FakeSender player) {
        if (!player.isPlayer()) {
            throw new IllegalArgumentException("sender is not a player");
        }
        players.put(player.getName(), player);
        return this;
    }

    public String permissionPrefix() {
        return permissionPrefix;
    }

    /**
     * Does nothing. Mocks only live while a command executes; this exists so tests can use try-with-resources like
     * the other CommandLib testing artifacts.
     */
    @Override
    public void close() {
    }

    /**
     * Keeps the server mock and the static stubs for one command execution.
     */
    private final class Scope implements AutoCloseable {
        private final MinecraftServer server;
        private final List<MockedStatic<?>> staticMocks = new ArrayList<>();

        private Scope(FakeSender sender) {
            Map<String, FakeSender> onlinePlayers = new LinkedHashMap<>(players);
            if (sender.isPlayer()) {
                onlinePlayers.putIfAbsent(sender.getName(), sender);
            }
            server = createServer(onlinePlayers);

            try {
                MockedStatic<CurrentServer> currentServer = Mockito.mockStatic(CurrentServer.class);
                staticMocks.add(currentServer);
                currentServer.when(CurrentServer::get)
                             .thenReturn(server);

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
                   *///?} elif forge && >=1.18 {
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
        }

        @Override
        public void close() {
            for (int i = staticMocks.size() - 1; i >= 0; i--) {
                staticMocks.get(i)
                           .close();
            }
            staticMocks.clear();
        }
    }

    private static boolean hasPermission(Map<String, FakeSender> onlinePlayers, Object player, String node) {
        return onlinePlayers.values()
                            .stream()
                            .filter(sender -> sender.asPlayer()
                                                    .orElse(null) == player)
                            .findFirst()
                            .map(sender -> sender.hasPermission(node))
                            .orElse(false);
    }

    private static MinecraftServer createServer(Map<String, FakeSender> onlinePlayers) {
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
        String[] playerNames = onlinePlayers.keySet()
                                            .toArray(new String[0]);
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
        //? if >=1.18 {
        WorldData worldData = Mockito.mock(WorldData.class);
        Mockito.when(worldData.enabledFeatures())
               .thenReturn(FeatureFlags.DEFAULT_FLAGS);
        Mockito.when(server.getWorldData())
               .thenReturn(worldData);
        //?}
        //? if >=1.20.5 {
        /*RegistryAccess.Frozen registryAccess = MinecraftBootstrap.registryAccess();
        Mockito.when(server.registryAccess())
               .thenReturn(registryAccess);
        *///?}
        return server;
    }

    private static Optional<FakeSender> findPlayer(Map<String, FakeSender> onlinePlayers, String name) {
        return onlinePlayers.values()
                            .stream()
                            .filter(sender -> sender.getName()
                                                    .equalsIgnoreCase(name))
                            .findFirst();
    }

    public static final class Builder {
        private final List<Supplier<? extends Command>> commandSuppliers = new ArrayList<>();
        private CommandScheduler scheduler = CommandScheduler.of(Runnable::run, Runnable::run);
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

        public CommandTester build() {
            return new CommandTester(this);
        }

        private List<Command> createCommands() {
            List<Command> commands = new ArrayList<>();
            for (Supplier<? extends Command> supplier : commandSuppliers) {
                commands.add(Objects.requireNonNull(supplier.get(), "command supplier returned null"));
            }
            return commands;
        }
    }

    static final class MinecraftBootstrap {
        private static boolean bootstrapped;
        //? if >=1.20.5 {
        /*@Nullable
        private static RegistryAccess.Frozen registryAccess;

        // The server's registries, including data-driven ones such as enchantments, built from vanilla's
        // registry bootstrap code instead of loading the vanilla data pack.
        private static synchronized RegistryAccess.Frozen registryAccess() {
            if (registryAccess != null) {
                return registryAccess;
            }
            List<Registry<?>> registries = new ArrayList<>();
            BuiltInRegistries.REGISTRY.forEach(registries::add);
            HolderLookup.Provider vanilla = VanillaRegistries.createLookup();
            vanilla.listRegistries()
                   .filter(key -> !BuiltInRegistries.REGISTRY.containsKey(key.location()))
                   .forEach(key -> registries.add(copy(vanilla.lookupOrThrow(key))));
            registryAccess = new RegistryAccess.ImmutableRegistryAccess(registries).freeze();
            return registryAccess;
        }

        @SuppressWarnings("unchecked")
        private static <T> Registry<T> copy(HolderLookup.RegistryLookup<T> lookup) {
            MappedRegistry<T> registry = new MappedRegistry<>((ResourceKey<? extends Registry<T>>) lookup.key(),
                                                              Lifecycle.stable());
            lookup.listElements()
                  .forEach(holder -> registry.register(holder.key(), holder.value(), RegistrationInfo.BUILT_IN));
            return registry.freeze();
        }
        *///?}

        static synchronized void ensureBootstrapped() {
            if (bootstrapped) {
                return;
            }
            //? if >=1.19
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
