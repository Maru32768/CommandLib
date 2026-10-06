package net.kunmc.lab.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import net.kunmc.lab.commandlib.nms.argument.*;
import net.kunmc.lab.commandlib.nms.command.MockNMSCommandListenerWrapper;
import net.kunmc.lab.commandlib.nms.resources.MockNMSCraftParticle;
import net.kunmc.lab.commandlib.nms.world.MockNMSCraftBlockData;
import net.kunmc.lab.commandlib.nms.world.MockNMSCraftEnchantment;
import net.kunmc.lab.commandlib.nms.world.MockNMSCraftItemStack;
import net.kunmc.lab.commandlib.nms.world.MockNMSCraftPotionEffectType;
import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.NMSReflection;
import net.kunmc.lab.commandlib.util.nms.argument.*;
import net.kunmc.lab.commandlib.util.nms.command.NMSCommandListenerWrapper;
import net.kunmc.lab.commandlib.util.nms.resources.NMSCraftParticle;
import net.kunmc.lab.commandlib.util.nms.world.NMSCraftBlockData;
import net.kunmc.lab.commandlib.util.nms.world.NMSCraftEnchantment;
import net.kunmc.lab.commandlib.util.nms.world.NMSCraftItemStack;
import net.kunmc.lab.commandlib.util.nms.world.NMSCraftPotionEffectType;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Executes commands in tests without a running Minecraft server.
 * Must be closed after use (use try-with-resources).
 */
public class CommandTester implements AutoCloseable {
    private static final ThreadLocal<CommandTester> current = new ThreadLocal<>();

    private final CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
    private final Map<String, Entity> fakeEntities = new LinkedHashMap<>();
    private final Map<String, World> fakeWorlds = new LinkedHashMap<>();
    private final ThreadLocal<CommandSender> currentCommandSender = new ThreadLocal<>();

    /**
     * For use by mock NMS classes only.
     */
    public static Entity getFakeEntity(String name) {
        return requireCurrent().fakeEntities.get(name);
    }

    /**
     * For use by mock NMS classes only. Returns the registered fake entities in registration order.
     */
    public static Collection<Entity> getFakeEntities() {
        return List.copyOf(requireCurrent().fakeEntities.values());
    }

    /**
     * For use by mock NMS classes only.
     */
    public static World getFakeWorld(String key) {
        return requireCurrent().fakeWorlds.get(key);
    }

    /**
     * For use by mock NMS classes only.
     */
    public static CommandSender getCurrentCommandSender() {
        CommandSender sender = requireCurrent().currentCommandSender.get();
        if (sender == null) {
            throw new IllegalStateException("No active CommandSender");
        }
        return sender;
    }

    private final MockedStatic<NMSReflection> nmsReflectionMock;
    private final MockedStatic<NMSClassRegistry> nmsRegistryMock;

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
     *     argument(new PlayerArgument("target")).execute((p, ctx) -> { ... });
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
        Objects.requireNonNull(builder.permissionPrefix, "permissionPrefix");
        if (builder.commandSuppliers.isEmpty()) {
            throw new IllegalStateException("At least one command must be registered.");
        }

        current.set(this);

        nmsReflectionMock = Mockito.mockStatic(NMSReflection.class);
        nmsReflectionMock.when(() -> NMSReflection.findMinecraftClass(Mockito.anyString(), Mockito.any()))
                         .thenReturn(Object.class);
        nmsReflectionMock.when(() -> NMSReflection.findCraftBukkitClass(Mockito.anyString()))
                         .thenReturn(Object.class);

        nmsRegistryMock = Mockito.mockStatic(NMSClassRegistry.class);
        nmsRegistryMock.when(() -> NMSClassRegistry.findClass(Mockito.any()))
                       .thenThrow(new UnsupportedOperationException("This NMS class is not supported in CommandTester."));

        builder.nmsMocks.forEach((lookUpClass, mockClass) -> registerNmsMock(nmsRegistryMock, lookUpClass, mockClass));

        try {
            new CommandNodeCreator<>(builder.createCommands(), builder.permissionPrefix).build()
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
        fakeEntities.put(player.getName(), player);
        return this;
    }

    /**
     * Registers a fake entity by name, looked up when resolving
     */
    public CommandTester withFakeEntity(String name, Entity entity) {
        fakeEntities.put(name, entity);
        return this;
    }

    /**
     * Registers a fake world by dimension key, e.g. {@code minecraft:overworld}.
     */
    public CommandTester withFakeWorld(String key, World world) {
        fakeWorlds.put(key, world);
        return this;
    }

    /**
     * Executes the given command input as the specified sender.
     *
     * @throws RuntimeException if the input does not match any registered command
     */
    public void execute(String input, CommandSender sender) {
        CommandTester previous = current.get();
        current.set(this);
        currentCommandSender.set(sender);
        try {
            dispatcher.execute(input, new Object());
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
    public void execute(String input, FakeSender sender) {
        execute(input, sender.getCommandSender());
    }

    /**
     * Returns Brigadier suggestions for the given partial input.
     */
    public CompletableFuture<Suggestions> suggestions(String input, FakeSender sender) {
        return suggestions(input, sender.getCommandSender());
    }

    /**
     * Returns Brigadier suggestions for the given partial input.
     */
    public CompletableFuture<Suggestions> suggestions(String input, CommandSender sender) {
        CommandTester previous = current.get();
        current.set(this);
        currentCommandSender.set(sender);
        try {
            ParseResults<Object> parseResults = dispatcher.parse(input, new Object());
            return dispatcher.getCompletionSuggestions(parseResults);
        } finally {
            currentCommandSender.remove();
            restoreCurrent(previous);
        }
    }

    @Override
    public void close() {
        nmsRegistryMock.close();
        nmsReflectionMock.close();
        if (current.get() == this) {
            current.remove();
        }
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
        private final Map<Class<? extends NMSClass>, Class<? extends NMSClass>> nmsMocks = defaultNmsMocks();
        private String permissionPrefix;

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

        public <T extends NMSClass> Builder mockNmsClass(Class<T> lookUpClass, Class<? extends T> mockClass) {
            nmsMocks.put(Objects.requireNonNull(lookUpClass, "lookUpClass"),
                         Objects.requireNonNull(mockClass, "mockClass"));
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
        mocks.put(NMSArgumentTypeRegistrar.class, MockNMSArgumentTypeRegistrar.class);
        return mocks;
    }
}
