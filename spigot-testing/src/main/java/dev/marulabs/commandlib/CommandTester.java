package dev.marulabs.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import dev.marulabs.commandlib.internal.nms.NMSClass;
import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.nms.argument.*;
import dev.marulabs.commandlib.internal.nms.command.NMSCommandListenerWrapper;
import dev.marulabs.commandlib.internal.nms.mock.argument.*;
import dev.marulabs.commandlib.internal.nms.mock.command.MockNMSCommandListenerWrapper;
import dev.marulabs.commandlib.internal.nms.mock.resources.MockNMSCraftParticle;
import dev.marulabs.commandlib.internal.nms.mock.world.MockNMSCraftBlockData;
import dev.marulabs.commandlib.internal.nms.mock.world.MockNMSCraftEnchantment;
import dev.marulabs.commandlib.internal.nms.mock.world.MockNMSCraftItemStack;
import dev.marulabs.commandlib.internal.nms.mock.world.MockNMSCraftPotionEffectType;
import dev.marulabs.commandlib.internal.nms.resources.NMSCraftParticle;
import dev.marulabs.commandlib.internal.nms.world.NMSCraftBlockData;
import dev.marulabs.commandlib.internal.nms.world.NMSCraftEnchantment;
import dev.marulabs.commandlib.internal.nms.world.NMSCraftItemStack;
import dev.marulabs.commandlib.internal.nms.world.NMSCraftPotionEffectType;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Executes commands in tests without a running Minecraft server.
 * Must be closed after use (use try-with-resources).
 */
public class CommandTester implements AutoCloseable {
    private static final ThreadLocal<CommandTester> current = new ThreadLocal<>();

    private final CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
    // Keyed by lower-case name: a server looks players up by name ignoring case.
    private final Map<String, Entity> fakeEntities = new LinkedHashMap<>();
    private final Map<String, World> fakeWorlds = new LinkedHashMap<>();
    // Per tester on purpose: each tester tracks the sender of the command it is executing.
    @SuppressWarnings("ThreadLocalUsage")
    private final ThreadLocal<CommandSender> currentCommandSender = new ThreadLocal<>();

    /**
     * For use by mock NMS classes only.
     */
    @ApiStatus.Internal
    @Nullable
    public static Entity getFakeEntity(String name) {
        return requireCurrent().fakeEntities.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * For use by mock NMS classes only. Returns the registered fake entities in registration order.
     */
    @ApiStatus.Internal
    public static Collection<Entity> getFakeEntities() {
        return List.copyOf(requireCurrent().fakeEntities.values());
    }

    /**
     * For use by mock NMS classes only.
     */
    @ApiStatus.Internal
    @Nullable
    public static World getFakeWorld(String key) {
        return requireCurrent().fakeWorlds.get(MockArgumentTypes.normalizeKey(key));
    }

    /**
     * For use by mock NMS classes only.
     */
    @ApiStatus.Internal
    public static CommandSender getCurrentCommandSender() {
        CommandSender sender = requireCurrent().currentCommandSender.get();
        if (sender == null) {
            throw new IllegalStateException("No active CommandSender");
        }
        return sender;
    }

    private final MockedStatic<NMSClassRegistry> nmsRegistryMock;
    private final Map<Class<? extends NMSClass>, Class<? extends NMSClass>> nmsMocks;
    private final Thread ownerThread = Thread.currentThread();
    private final List<Command> commands = new ArrayList<>();
    private final String permissionPrefix;

    public static Builder builder() {
        return new Builder();
    }

    public CommandTester(Command command, String permissionPrefix) {
        this(builder().command(command)
                      .permissionPrefix(permissionPrefix));
    }

    /**
     * Creates a {@code CommandTester} where NMS mocks are active when the command is built.
     * Use this overload when the command contains NMS-based arguments (e.g. {@code PlayerArgument})
     * whose constructors call into {@code NMSClassRegistry} before the tester is ready.
     *
     * <pre>{@code
     * new CommandTester(() -> new Command("heal") {{
     *     argument(new PlayerArgument("target")).execute((ctx, p) -> { ... });
     * }}, "myplugin.command")
     * }</pre>
     */
    public CommandTester(Supplier<? extends Command> commandSupplier, String permissionPrefix) {
        this(builder().command(commandSupplier)
                      .permissionPrefix(permissionPrefix));
    }

    public CommandTester(Collection<? extends Command> commands, String permissionPrefix) {
        this(builder().commands(commands)
                      .permissionPrefix(permissionPrefix));
    }

    private CommandTester(Builder builder) {
        permissionPrefix = Objects.requireNonNull(builder.permissionPrefix, "permissionPrefix");
        if (builder.commandSuppliers.isEmpty()) {
            throw new IllegalStateException("At least one command must be registered.");
        }

        current.set(this);

        nmsMocks = Map.copyOf(builder.nmsMocks);
        nmsRegistryMock = mockNmsRegistry();

        try {
            commands.addAll(builder.createCommands());
            CommandScheduler scheduler = builder.scheduler;
            CommandScheduler onAnyThread = new CommandScheduler() {
                @Override
                public Executor sync(CommonCommandContext<?, ?> ctx) {
                    return withMocks(scheduler.sync(ctx));
                }

                @Override
                public Executor async(CommonCommandContext<?, ?> ctx) {
                    return withMocks(scheduler.async(ctx));
                }
            };
            new CommandNodeCreator<>(commands, permissionPrefix, onAnyThread).build()
                                                                             .forEach(dispatcher.getRoot()::addChild);
        } catch (Exception e) {
            close();
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    /**
     * Registers a fake player, looked up by name when resolving
     */
    public CommandTester withFakePlayer(Player player) {
        fakeEntities.put(player.getName()
                               .toLowerCase(Locale.ROOT),
                         player);
        return this;
    }

    /**
     * Registers a fake entity by name, looked up when resolving
     */
    public CommandTester withFakeEntity(String name, Entity entity) {
        fakeEntities.put(name.toLowerCase(Locale.ROOT), entity);
        return this;
    }

    /**
     * Registers a fake world by dimension key, e.g. {@code minecraft:overworld}. A key without a namespace is in
     * {@code minecraft}, as on a server.
     */
    public CommandTester withFakeWorld(String key, World world) {
        fakeWorlds.put(MockArgumentTypes.normalizeKey(key), world);
        return this;
    }

    boolean hasFakePlayer(String name) {
        return fakeEntities.containsKey(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Executes the given command input as the specified sender and returns the command's result, which
     * {@code /execute store result} would store. A command that fails sends its failure messages to the sender, as the
     * server does, and returns 0.
     * An {@link AssertionError} the command throws, such as a failed assertion in an executor, is rethrown after them.
     *
     * @throws RuntimeException if the input does not match any registered command
     */
    public int execute(String input, CommandSender sender) {
        CommandTester previous = current.get();
        current.set(this);
        currentCommandSender.set(sender);
        try {
            // The sender is the source, so a follow-up of an async command still finds it after execute returns.
            return CommandFailure.replayed(() -> dispatcher.execute(input, sender));
        } catch (CommandFailure e) {
            e.sendMessages();
            e.rethrowAssertionError();
            return 0;
        } catch (CommandSyntaxException e) {
            throw new RuntimeException("Command syntax error: " + e.getMessage(), e);
        } finally {
            currentCommandSender.remove();
            restoreCurrent(previous);
        }
    }

    /**
     * Executes the given command input as the specified sender.
     * Messages sent during execution are captured in {@link FakeSender#getSentMessages()}.
     *
     * @throws RuntimeException if the input does not match any registered command
     */
    public int execute(String input, FakeSender sender) {
        return execute(input, sender.getCommandSender());
    }

    /**
     * Returns the suggestions a client gets for the given partial input. Like the client's command tree, they leave out
     * the nodes the sender cannot use.
     */
    public CompletableFuture<Suggestions> suggestions(String input, FakeSender sender) {
        return suggestions(input, sender.getCommandSender());
    }

    /**
     * Returns the suggestions a client gets for the given partial input, like
     * {@link #suggestions(String, FakeSender)}.
     */
    public CompletableFuture<Suggestions> suggestions(String input, CommandSender sender) {
        CommandTester previous = current.get();
        current.set(this);
        currentCommandSender.set(sender);
        try {
            // The sender is the source, as in execute, so a suggestion that completes later still finds it.
            ParseResults<Object> parseResults = dispatcher.parse(input, sender);
            return ClientSuggestions.complete(parseResults);
        } finally {
            currentCommandSender.remove();
            restoreCurrent(previous);
        }
    }

    /**
     * Returns the texts of the suggestions for the given partial input, waiting for them to complete.
     */
    public List<String> suggest(String input, FakeSender sender) {
        return suggest(input, sender.getCommandSender());
    }

    /**
     * Returns the texts of the suggestions for the given partial input, waiting for them to complete.
     */
    public List<String> suggest(String input, CommandSender sender) {
        return suggestions(input, sender).join()
                                         .getList()
                                         .stream()
                                         .map(Suggestion::getText)
                                         .collect(Collectors.toList());
    }

    /**
     * Renders the registered commands as indented text: each command with its aliases and permission node, then its
     * cooldown bypass permission, options, argument branches and child commands. Executors, descriptions,
     * prerequisites and {@code visibleWhen} predicates are not shown.
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
    public void assertCommandTreeMatchesSnapshot(String snapshotName) {
        Snapshots.assertMatches(snapshotName, commandTree());
    }

    @Override
    public void close() {
        nmsRegistryMock.close();
        if (current.get() == this) {
            current.remove();
        }
    }

    private MockedStatic<NMSClassRegistry> mockNmsRegistry() {
        MockedStatic<NMSClassRegistry> registryMock = Mockito.mockStatic(NMSClassRegistry.class);
        registryMock.when(() -> NMSClassRegistry.findClass(Mockito.any()))
                    .thenThrow(new UnsupportedOperationException("This NMS class is not supported in CommandTester."));
        nmsMocks.forEach((lookUpClass, mockClass) -> registerNmsMock(registryMock, lookUpClass, mockClass));
        // The mocks stand in for the typed NMS modules, so diagnostics report that none is in use.
        registryMock.when(NMSClassRegistry::moduleStatus)
                    .thenReturn(List.of());
        return registryMock;
    }

    /**
     * Makes this tester and its NMS mocks, which Mockito keeps per thread, active while a task of the scheduler runs,
     * so an executor on another thread reaches the sender too.
     */
    private Executor withMocks(Executor executor) {
        return task -> executor.execute(() -> {
            CommandTester previous = current.get();
            if (previous == this) {
                task.run();
                return;
            }
            current.set(this);
            try {
                if (Thread.currentThread() == ownerThread) {
                    task.run();
                } else {
                    try (MockedStatic<NMSClassRegistry> ignored = mockNmsRegistry()) {
                        task.run();
                    }
                }
            } finally {
                restoreCurrent(previous);
            }
        });
    }

    private static CommandTester requireCurrent() {
        CommandTester tester = current.get();
        if (tester == null) {
            throw new IllegalStateException("No active CommandTester");
        }
        return tester;
    }

    private static void restoreCurrent(CommandTester previous) {
        if (previous == null) {
            current.remove();
        } else {
            current.set(previous);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerNmsMock(MockedStatic<NMSClassRegistry> registryMock,
                                        Class<? extends NMSClass> lookUpClass,
                                        Class<? extends NMSClass> mockClass) {
        registryMock.when(() -> NMSClassRegistry.findClass((Class) lookUpClass))
                    .thenAnswer(inv -> mockClass);
    }

    public static class Builder {
        private final List<Supplier<? extends Command>> commandSuppliers = new ArrayList<>();
        private CommandScheduler scheduler = CommandScheduler.of(Runnable::run, Runnable::run);
        private CommandDefaults<CommandContext> defaults = CommandDefaults.of(CommandContext.class);
        private final Map<Class<? extends NMSClass>, Class<? extends NMSClass>> nmsMocks = defaultNmsMocks();
        private @Nullable String permissionPrefix;

        public Builder command(Command command) {
            Objects.requireNonNull(command, "command");
            return command(() -> command);
        }

        public Builder command(Supplier<? extends Command> commandSupplier) {
            commandSuppliers.add(Objects.requireNonNull(commandSupplier, "commandSupplier"));
            return this;
        }

        public Builder commands(Collection<? extends Command> commands) {
            Objects.requireNonNull(commands, "commands");
            commands.forEach(this::command);
            return this;
        }

        public Builder permissionPrefix(String permissionPrefix) {
            this.permissionPrefix = Objects.requireNonNull(permissionPrefix, "permissionPrefix");
            return this;
        }

        /**
         * Sets the threads of {@code executeAsync} commands. By default both executors run tasks at once, so an async
         * command completes before {@code execute} returns; pass executors that queue tasks to test what happens in
         * between. An executor may also run tasks on threads of its own, since the tester activates its NMS mocks on
         * the thread of each task.
         */
        public Builder scheduler(@NotNull CommandScheduler scheduler) {
            this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
            return this;
        }

        public <T extends NMSClass> Builder mockNmsClass(Class<T> lookUpClass, Class<? extends T> mockClass) {
            nmsMocks.put(Objects.requireNonNull(lookUpClass, "lookUpClass"),
                         Objects.requireNonNull(mockClass, "mockClass"));
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

        private Collection<? extends Command> createCommands() {
            List<Command> commands = new ArrayList<>();
            for (Supplier<? extends Command> commandSupplier : commandSuppliers) {
                commands.add(commandSupplier.get());
            }
            defaults.applyTo(commands);
            return commands;
        }
    }

    private static Map<Class<? extends NMSClass>, Class<? extends NMSClass>> defaultNmsMocks() {
        Map<Class<? extends NMSClass>, Class<? extends NMSClass>> mocks = new LinkedHashMap<>();
        mocks.put(NMSCommandListenerWrapper.class, MockNMSCommandListenerWrapper.class);
        mocks.put(NMSArgumentPlayer.class, MockNMSArgumentPlayer.class);
        mocks.put(NMSArgumentPlayers.class, MockNMSArgumentPlayers.class);
        mocks.put(NMSArgumentEntity.class, MockNMSArgumentEntity.class);
        mocks.put(NMSArgumentEntities.class, MockNMSArgumentEntities.class);
        mocks.put(NMSArgumentProfile.class, MockNMSArgumentProfile.class);
        mocks.put(NMSArgumentScoreboardTeam.class, MockNMSArgumentScoreboardTeam.class);
        mocks.put(NMSArgumentDimension.class, MockNMSArgumentDimension.class);
        mocks.put(NMSArgumentVec3D.class, MockNMSArgumentVec3D.class);
        mocks.put(NMSArgumentBlockPosition.class, MockNMSArgumentBlockPosition.class);
        mocks.put(NMSArgumentItemPredicate.class, MockNMSArgumentItemPredicate.class);
        mocks.put(NMSArgumentBlockPredicate.class, MockNMSArgumentBlockPredicate.class);
        mocks.put(NMSArgumentIntRange.class, MockNMSArgumentIntRange.class);
        mocks.put(NMSArgumentFloatRange.class, MockNMSArgumentFloatRange.class);
        mocks.put(NMSArgumentTime.class, MockNMSArgumentTime.class);
        mocks.put(NMSArgumentScoreboardSlot.class, MockNMSArgumentScoreboardSlot.class);
        mocks.put(NMSArgumentChatFormat.class, MockNMSArgumentChatFormat.class);
        mocks.put(NMSArgumentEnchantment.class, MockNMSArgumentEnchantment.class);
        mocks.put(NMSArgumentNamespacedKey.class, MockNMSArgumentNamespacedKey.class);
        mocks.put(NMSCraftEnchantment.class, MockNMSCraftEnchantment.class);
        mocks.put(NMSArgumentMobEffect.class, MockNMSArgumentMobEffect.class);
        mocks.put(NMSCraftPotionEffectType.class, MockNMSCraftPotionEffectType.class);
        mocks.put(NMSArgumentParticle.class, MockNMSArgumentParticle.class);
        mocks.put(NMSCraftParticle.class, MockNMSCraftParticle.class);
        mocks.put(NMSArgumentItemStack.class, MockNMSArgumentItemStack.class);
        mocks.put(NMSCraftItemStack.class, MockNMSCraftItemStack.class);
        mocks.put(NMSArgumentTile.class, MockNMSArgumentTile.class);
        mocks.put(NMSCraftBlockData.class, MockNMSCraftBlockData.class);
        mocks.put(NMSArgumentChatComponent.class, MockNMSArgumentChatComponent.class);
        mocks.put(NMSArgumentChat.class, MockNMSArgumentChat.class);
        mocks.put(NMSArgumentNBTTag.class, MockNMSArgumentNBTTag.class);
        mocks.put(NMSArgumentTypeRegistrar.class, MockNMSArgumentTypeRegistrar.class);
        return mocks;
    }
}
