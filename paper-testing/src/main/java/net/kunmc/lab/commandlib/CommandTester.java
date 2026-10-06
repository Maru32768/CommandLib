package net.kunmc.lab.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.math.Position;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Executes Paper commands in tests without a running Minecraft server.
 */
@SuppressWarnings("UnstableApiUsage")
public final class CommandTester implements AutoCloseable {
    private static final ThreadLocal<CommandTester> current = new ThreadLocal<>();

    private final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
    private final Map<String, Entity> fakeEntities = new LinkedHashMap<>();
    private final Map<String, Player> fakePlayers = new LinkedHashMap<>();
    private final Map<String, World> fakeWorlds = new LinkedHashMap<>();
    private BlockData fakeBlockData = Mockito.mock(BlockData.class);
    private ItemStack fakeItemStack = Mockito.mock(ItemStack.class);
    // These registry value classes trigger Paper/Bukkit RegistryAccess initialization when
    // touched directly outside a real Paper server. Tests may inject real/bootstrap-safe
    // values if available, but paper-testing intentionally does not create defaults.
    private Enchantment fakeEnchantment;
    private Particle fakeParticle = Particle.POOF;
    private PotionEffectType fakePotionEffectType;
    private Biome fakeBiome;
    private Attribute fakeAttribute;
    private EntityType fakeEntityType = EntityType.ZOMBIE;
    private Sound fakeSound;
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

        try (MockedStatic<ArgumentTypes> ignored = mockArgumentTypes()) {
            new CommandNodeCreator<>(builder.createCommands(), permissionPrefix).build()
                                                                                .forEach(dispatcher.getRoot()::addChild);
        }
    }

    /**
     * Executes the given command input as the specified fake sender.
     *
     * @throws RuntimeException if the input does not match any registered command
     */
    public void execute(@NotNull String input, @NotNull FakeSender sender) {
        execute(input, sender.asSender());
    }

    /**
     * Executes the given command input as the specified Bukkit/Paper sender.
     *
     * @throws RuntimeException if the input does not match any registered command
     */
    public void execute(@NotNull String input, @NotNull CommandSender sender) {
        CommandTester previous = current.get();
        current.set(this);
        try {
            dispatcher.execute(input, commandSource(sender));
        } catch (CommandSyntaxException e) {
            throw new RuntimeException("Command syntax error: " + e.getMessage(), e);
        } finally {
            restoreCurrent(previous);
        }
    }

    /**
     * Returns Brigadier suggestions for the given partial input.
     */
    public CompletableFuture<Suggestions> suggestions(@NotNull String input, @NotNull FakeSender sender) {
        return suggestions(input, sender.asSender());
    }

    /**
     * Returns Brigadier suggestions for the given partial input.
     */
    public CompletableFuture<Suggestions> suggestions(@NotNull String input, @NotNull CommandSender sender) {
        CommandTester previous = current.get();
        current.set(this);
        try {
            ParseResults<CommandSourceStack> parseResults = dispatcher.parse(input, commandSource(sender));
            return dispatcher.getCompletionSuggestions(parseResults);
        } finally {
            restoreCurrent(previous);
        }
    }

    public String permissionPrefix() {
        return permissionPrefix;
    }

    public CommandTester withFakePlayer(@NotNull Player player) {
        fakePlayers.put(player.getName(), player);
        fakeEntities.put(player.getName(), player);
        return this;
    }

    public CommandTester withFakeEntity(@NotNull String name, @NotNull Entity entity) {
        fakeEntities.put(name, entity);
        return this;
    }

    public CommandTester withFakeBlockData(@NotNull BlockData blockData) {
        this.fakeBlockData = blockData;
        return this;
    }

    public CommandTester withFakeItemStack(@NotNull ItemStack itemStack) {
        this.fakeItemStack = itemStack;
        return this;
    }

    public CommandTester withFakeEnchantment(@NotNull Enchantment enchantment) {
        this.fakeEnchantment = enchantment;
        return this;
    }

    public CommandTester withFakeParticle(@NotNull Particle particle) {
        this.fakeParticle = particle;
        return this;
    }

    public CommandTester withFakePotionEffectType(@NotNull PotionEffectType potionEffectType) {
        this.fakePotionEffectType = potionEffectType;
        return this;
    }

    public CommandTester withFakeBiome(@NotNull Biome biome) {
        this.fakeBiome = biome;
        return this;
    }

    public CommandTester withFakeAttribute(@NotNull Attribute attribute) {
        this.fakeAttribute = attribute;
        return this;
    }

    public CommandTester withFakeEntityType(@NotNull EntityType entityType) {
        this.fakeEntityType = entityType;
        return this;
    }

    public CommandTester withFakeSound(@NotNull Sound sound) {
        this.fakeSound = sound;
        return this;
    }

    public CommandTester withFakeWorld(@NotNull World world) {
        fakeWorlds.put(world.getName(), world);
        return this;
    }

    @Override
    public void close() {
        if (current.get() == this) {
            current.remove();
        }
    }

    private static CommandSourceStack commandSource(CommandSender sender) {
        CommandSourceStack source = Mockito.mock(CommandSourceStack.class);
        Mockito.when(source.getSender())
               .thenReturn(sender);
        Mockito.when(source.getLocation())
               .thenReturn(new Location(null, 0, 0, 0));
        return source;
    }

    private MockedStatic<ArgumentTypes> mockArgumentTypes() {
        MockedStatic<ArgumentTypes> argumentTypes = Mockito.mockStatic(ArgumentTypes.class);
        argumentTypes.when(ArgumentTypes::gameMode)
                     .thenReturn(word(token -> GameMode.valueOf(token.toUpperCase())));
        argumentTypes.when(ArgumentTypes::namespacedKey)
                     .thenReturn(word(NamespacedKey::fromString));
        argumentTypes.when(ArgumentTypes::world)
                     .thenReturn(word(token -> requireCurrent().fakeWorlds.get(token)));
        argumentTypes.when(ArgumentTypes::entity)
                     .thenReturn(word(token -> entityResolver(token, false)));
        argumentTypes.when(ArgumentTypes::entities)
                     .thenReturn(word(token -> entityResolver(token, true)));
        argumentTypes.when(ArgumentTypes::player)
                     .thenReturn(word(token -> playerResolver(token, false)));
        argumentTypes.when(ArgumentTypes::players)
                     .thenReturn(word(token -> playerResolver(token, true)));
        argumentTypes.when(ArgumentTypes::blockState)
                     .thenReturn(word(token -> {
                         BlockState blockState = Mockito.mock(BlockState.class);
                         Mockito.when(blockState.getBlockData())
                                .thenReturn(requireCurrent().fakeBlockData);
                         return blockState;
                     }));
        argumentTypes.when(ArgumentTypes::finePosition)
                     .thenReturn(finePosition());
        argumentTypes.when(() -> ArgumentTypes.finePosition(Mockito.anyBoolean()))
                     .thenReturn(finePosition());
        argumentTypes.when(ArgumentTypes::itemStack)
                     .thenReturn(word(token -> requireCurrent().fakeItemStack));
        argumentTypes.when(() -> ArgumentTypes.resource(Mockito.any(RegistryKey.class)))
                     .thenAnswer(invocation -> word(token -> resourceValue(invocation.getArgument(0))));
        return argumentTypes;
    }

    private EntitySelectorArgumentResolver entityResolver(String token, boolean multiple) {
        return source -> {
            CommandTester tester = requireCurrent();
            if (multiple && "@a".equals(token)) {
                return new ArrayList<>(tester.fakeEntities.values());
            }
            Entity entity = tester.fakeEntities.get(token);
            return entity == null ? List.of() : List.of(entity);
        };
    }

    private PlayerSelectorArgumentResolver playerResolver(String token, boolean multiple) {
        return source -> {
            CommandTester tester = requireCurrent();
            if (multiple && "@a".equals(token)) {
                return new ArrayList<>(tester.fakePlayers.values());
            }
            Player player = tester.fakePlayers.get(token);
            return player == null ? List.of() : List.of(player);
        };
    }

    private Object resourceValue(RegistryKey<?> key) {
        CommandTester tester = requireCurrent();
        if (RegistryKey.ENCHANTMENT.equals(key)) {
            return tester.fakeEnchantment;
        }
        if (RegistryKey.PARTICLE_TYPE.equals(key)) {
            return tester.fakeParticle;
        }
        if (RegistryKey.MOB_EFFECT.equals(key)) {
            return tester.fakePotionEffectType;
        }
        if (RegistryKey.BIOME.equals(key)) {
            return tester.fakeBiome;
        }
        if (RegistryKey.ATTRIBUTE.equals(key)) {
            return tester.fakeAttribute;
        }
        if (RegistryKey.ENTITY_TYPE.equals(key)) {
            return tester.fakeEntityType;
        }
        if (RegistryKey.SOUND_EVENT.equals(key)) {
            return tester.fakeSound;
        }
        throw new UnsupportedOperationException("Unsupported Paper registry argument in CommandTester: " + key);
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

    private static <T> ArgumentType<T> word(Function<String, T> parser) {
        return reader -> {
            int start = reader.getCursor();
            String token = readUntilWhitespace(reader);
            T value = parser.apply(token);
            if (value == null) {
                // Paper's registry and world arguments reject unknown values while parsing. Returning null here would
                // let Brigadier fail with a NullPointerException instead.
                reader.setCursor(start);
                throw new SimpleCommandExceptionType(new LiteralMessage("Unknown value '" + token + "'")).createWithContext(
                        reader);
            }
            return value;
        };
    }

    private static String readUntilWhitespace(StringReader reader) {
        int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            reader.skip();
        }
        return reader.getString()
                     .substring(start, reader.getCursor());
    }

    private static ArgumentType<FinePositionResolver> finePosition() {
        return reader -> {
            double x = readDouble(reader);
            reader.skipWhitespace();
            double y = readDouble(reader);
            reader.skipWhitespace();
            double z = readDouble(reader);
            return source -> Position.fine(x, y, z);
        };
    }

    private static double readDouble(StringReader reader) throws CommandSyntaxException {
        return reader.readDouble();
    }

    public static final class Builder {
        private final List<Supplier<? extends Command>> commandSuppliers = new ArrayList<>();
        private String permissionPrefix;

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
}
