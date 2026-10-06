package net.kunmc.lab.commandlib;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.mockito.MockedStatic;

//? if >=1.20.5 {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.registries.VanillaRegistries;
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
import net.neoforged.neoforge.server.ServerLifecycleHooks;
*///?} elif >=1.17 {
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.server.ServerLifecycleHooks;
//?} else
/*import net.minecraftforge.fml.server.ServerLifecycleHooks;*/
//? if >=1.18 {
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.WorldData;
//?} else
/*import net.minecraftforge.server.permission.PermissionAPI;*/

/**
 * Runs CommandLib Forge commands without a Minecraft server.
 *
 * <p>Minecraft's registries are bootstrapped once per JVM, so vanilla argument types such as items, blocks, and
 * effects parse for real. The server is a Mockito mock whose player list contains the fake players added with
 * {@link #withPlayer(FakeSender)} and the player executing each command, so player and entity arguments resolve those
 * players by name. Permission nodes are answered by the executing {@link FakeSender}.</p>
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
        new CommandNodeCreator<>(builder.createCommands(), permissionPrefix).build()
                                                                            .forEach(dispatcher.getRoot()::addChild);
    }

    /**
     * Executes the given command input as the specified fake sender.
     *
     * @throws RuntimeException if the input does not match any registered command
     */
    public void execute(@NotNull String input, @NotNull FakeSender sender) {
        try (Scope scope = new Scope(sender)) {
            dispatcher.execute(input, sender.createSource(scope.server));
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
                MockedStatic<ServerLifecycleHooks> lifecycleHooks = Mockito.mockStatic(ServerLifecycleHooks.class);
                staticMocks.add(lifecycleHooks);
                lifecycleHooks.when(ServerLifecycleHooks::getCurrentServer)
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
               .thenAnswer(invocation -> findPlayer(onlinePlayers, invocation.getArgument(0)).flatMap(FakeSender::asPlayer)
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

    private static final class MinecraftBootstrap {
        private static boolean bootstrapped;
        //? if >=1.20.5 {
        /*private static RegistryAccess.Frozen registryAccess;

        // The server's registries, including data-driven ones such as enchantments, built from vanilla's
        // registry bootstrap code instead of loading the vanilla data pack.
        private static synchronized RegistryAccess.Frozen registryAccess() {
            if (registryAccess != null) {
                return registryAccess;
            }
            HolderLookup.Provider vanilla = VanillaRegistries.createLookup();
            RegistryAccess.Frozen access = Mockito.mock(RegistryAccess.Frozen.class, Mockito.CALLS_REAL_METHODS);
            Mockito.doAnswer(invocation -> vanilla.lookup(invocation.getArgument(0)))
                   .when(access)
                   .lookup(Mockito.any());
            Mockito.doAnswer(invocation -> vanilla.listRegistries())
                   .when(access)
                   .listRegistries();
            registryAccess = access;
            return access;
        }
        *///?}

        private static synchronized void ensureBootstrapped() {
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
            *///?} elif >=1.17 {
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
