package dev.marulabs.commandlib;

import com.google.common.collect.Range;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.marulabs.commandlib.util.brigadier.VanillaArgumentTypes;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.SignedMessageResolver;
import io.papermc.paper.command.brigadier.argument.predicate.ItemStackPredicate;
import io.papermc.paper.command.brigadier.argument.range.DoubleRangeProvider;
import io.papermc.paper.command.brigadier.argument.range.IntegerRangeProvider;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.math.Position;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Executes Paper commands in tests without a running Minecraft server.
 */
@SuppressWarnings("UnstableApiUsage")
public final class CommandTester implements AutoCloseable {
    private static final ThreadLocal<CommandTester> current = new ThreadLocal<>();

    private final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
    // Both keyed by lower-case name: a server looks players up by name ignoring case.
    private final Map<String, Entity> fakeEntities = new LinkedHashMap<>();
    private final Map<String, Player> fakePlayers = new LinkedHashMap<>();
    private final Map<String, World> fakeWorlds = new LinkedHashMap<>();
    private BlockData fakeBlockData = Mockito.mock(BlockData.class);
    private ItemStack fakeItemStack = Mockito.mock(ItemStack.class);
    private final Map<RegistryKey<?>, Map<String, Object>> fakeRegistryValues = new LinkedHashMap<>();
    private final String permissionPrefix;
    private final List<Command> commands = new ArrayList<>();

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

        try (MockedStatic<ArgumentTypes> ignored = mockArgumentTypes();
             MockedStatic<VanillaArgumentTypes> ignoredVanilla = mockVanillaArgumentTypes()) {
            commands.addAll(builder.createCommands());
            new CommandNodeCreator<>(commands, permissionPrefix, builder.scheduler).build()
                                                                                   .forEach(dispatcher.getRoot()::addChild);
        }
    }

    boolean hasFakePlayer(String name) {
        return fakePlayers.containsKey(name.toLowerCase(Locale.ROOT));
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
        return execute(input, sender.asSender());
    }

    /**
     * Executes the given command input as the specified Bukkit/Paper sender, like {@link #execute(String, FakeSender)}.
     *
     * @throws RuntimeException if the input does not match any registered command
     */
    public int execute(@NotNull String input, @NotNull CommandSender sender) {
        CommandTester previous = current.get();
        current.set(this);
        try {
            return dispatcher.execute(input, commandSource(sender));
        } catch (CommandFailure e) {
            e.sendMessages();
            e.rethrowAssertionError();
            return 0;
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

    /**
     * Returns the texts of the suggestions for the given partial input, waiting for them to complete.
     */
    public List<String> suggest(@NotNull String input, @NotNull FakeSender sender) {
        return suggest(input, sender.asSender());
    }

    /**
     * Returns the texts of the suggestions for the given partial input, waiting for them to complete.
     */
    public List<String> suggest(@NotNull String input, @NotNull CommandSender sender) {
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

    public CommandTester withFakePlayer(@NotNull Player player) {
        String key = player.getName()
                           .toLowerCase(Locale.ROOT);
        fakePlayers.put(key, player);
        fakeEntities.put(key, player);
        return this;
    }

    public CommandTester withFakeEntity(@NotNull String name, @NotNull Entity entity) {
        fakeEntities.put(name.toLowerCase(Locale.ROOT), entity);
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

    /**
     * Adds a value that {@code ArgumentTypes.resource(registryKey)}, and so {@code RegistryArgument}, returns for its
     * key, such as {@code minecraft:sharpness}. Other keys fail while parsing, as on a server, so a registry without
     * values here accepts no key.
     */
    public <T> CommandTester withFakeRegistryValue(@NotNull RegistryKey<T> registryKey,
                                                   @NotNull String key,
                                                   @NotNull T value) {
        fakeRegistryValues.computeIfAbsent(registryKey, x -> new LinkedHashMap<>())
                          .put(Objects.requireNonNull(NamespacedKey.fromString(key), "key")
                                      .toString(),
                               value);
        return this;
    }

    /**
     * Registers a fake world by dimension key, e.g. {@code minecraft:overworld}, which {@code WorldArgument} takes as
     * the server does.
     */
    public CommandTester withFakeWorld(@NotNull String key, @NotNull World world) {
        fakeWorlds.put(Objects.requireNonNull(NamespacedKey.fromString(key), "key")
                              .toString(),
                       world);
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
        // An entity's source is at the entity, as on a server.
        Location location = sender instanceof Entity ? ((Entity) sender).getLocation() : null;
        Mockito.when(source.getLocation())
               .thenReturn(location != null ? location : new Location(null, 0, 0, 0));
        Mockito.when(source.getExecutor())
               .thenReturn(sender instanceof Entity ? (Entity) sender : null);
        return source;
    }

    private MockedStatic<ArgumentTypes> mockArgumentTypes() {
        MockedStatic<ArgumentTypes> argumentTypes = Mockito.mockStatic(ArgumentTypes.class);
        argumentTypes.when(ArgumentTypes::gameMode)
                     .thenReturn(word(CommandTester::gameMode));
        argumentTypes.when(ArgumentTypes::scoreboardDisplaySlot)
                     .thenReturn(word(DisplaySlot.NAMES::value));
        argumentTypes.when(ArgumentTypes::namespacedKey)
                     .thenReturn(word(NamespacedKey::fromString));
        argumentTypes.when(ArgumentTypes::world)
                     .thenReturn(word(token -> {
                         NamespacedKey key = NamespacedKey.fromString(token);
                         return key == null ? null : requireCurrent().fakeWorlds.get(key.toString());
                     }));
        argumentTypes.when(ArgumentTypes::entity)
                     .thenReturn(selector(true,
                                          false,
                                          token -> (EntitySelectorArgumentResolver) source -> select(token, source)));
        argumentTypes.when(ArgumentTypes::entities)
                     .thenReturn(selector(false,
                                          false,
                                          token -> (EntitySelectorArgumentResolver) source -> select(token, source)));
        argumentTypes.when(ArgumentTypes::player)
                     .thenReturn(selector(true,
                                          true,
                                          token -> (PlayerSelectorArgumentResolver) source -> players(select(token,
                                                                                                             source))));
        argumentTypes.when(ArgumentTypes::players)
                     .thenReturn(selector(false,
                                          true,
                                          token -> (PlayerSelectorArgumentResolver) source -> players(select(token,
                                                                                                             source))));
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
        argumentTypes.when(ArgumentTypes::blockPosition)
                     .thenReturn(blockPosition());
        argumentTypes.when(ArgumentTypes::itemPredicate)
                     .thenReturn(material("item",
                                          material -> (ItemStackPredicate) item -> item != null
                                                  && item.getType() == material));
        stubBlockInWorldPredicate(argumentTypes);
        argumentTypes.when(ArgumentTypes::integerRange)
                     .thenReturn(range(Integer::valueOf, range -> (IntegerRangeProvider) () -> range));
        argumentTypes.when(ArgumentTypes::doubleRange)
                     .thenReturn(range(Double::valueOf, range -> (DoubleRangeProvider) () -> range));
        argumentTypes.when(ArgumentTypes::time)
                     .thenReturn(time(0));
        argumentTypes.when(() -> ArgumentTypes.time(Mockito.anyInt()))
                     .thenAnswer(invocation -> time(invocation.getArgument(0)));
        argumentTypes.when(() -> ArgumentTypes.resource(Mockito.any(RegistryKey.class)))
                     .thenAnswer(invocation -> word(token -> resourceValue(invocation.getArgument(0), token)));
        argumentTypes.when(ArgumentTypes::component)
                     .thenReturn(component());
        argumentTypes.when(ArgumentTypes::signedMessage)
                     .thenReturn(signedMessage());
        return argumentTypes;
    }

    private static MockedStatic<VanillaArgumentTypes> mockVanillaArgumentTypes() {
        MockedStatic<VanillaArgumentTypes> vanillaArgumentTypes = Mockito.mockStatic(VanillaArgumentTypes.class);
        // Finds the end of the compound but does not check its contents, and returns it as written.
        ArgumentType<String> compoundTag = reader -> {
            if (!reader.canRead() || reader.peek() != '{') {
                throw syntaxError("Expected '{'", reader);
            }
            return readValue(reader);
        };
        vanillaArgumentTypes.when(VanillaArgumentTypes::compoundTag)
                            .thenReturn(compoundTag);
        // An enum with vanilla ChatFormatting's names, like the value of the vanilla color argument.
        ArgumentType<ChatColor> color = reader -> {
            int start = reader.getCursor();
            String token = readUntilWhitespace(reader);
            String key = colorKey(token);
            ChatColor format = Arrays.stream(ChatColor.values())
                                     .filter(x -> x.isColor() || x == ChatColor.RESET)
                                     .filter(x -> colorKey(x.name()).equals(key))
                                     .findFirst()
                                     .orElse(null);
            if (format == null) {
                reader.setCursor(start);
                throw syntaxError("Invalid color '" + token + "'", reader);
            }
            return format;
        };
        vanillaArgumentTypes.when(VanillaArgumentTypes::color)
                            .thenReturn(color);
        return vanillaArgumentTypes;
    }

    /**
     * Reads JSON text with Adventure, so the SNBT-only syntax of 1.21.5 (unquoted keys) is not supported.
     */
    private static ArgumentType<Component> component() {
        return reader -> {
            int start = reader.getCursor();
            String json = readValue(reader);
            try {
                return GsonComponentSerializer.gson()
                                              .deserialize(json);
            } catch (RuntimeException e) {
                // Gson reports malformed JSON and values of the wrong shape with different runtime exceptions.
                reader.setCursor(start);
                throw syntaxError("Invalid chat component: " + e.getMessage(), reader);
            }
        };
    }

    /**
     * Reads the rest of the input. Selectors are kept as written, and the message resolves as an unsigned system
     * message.
     */
    private static ArgumentType<SignedMessageResolver> signedMessage() {
        return reader -> {
            String text = reader.getRemaining();
            reader.setCursor(reader.getTotalLength());
            return new SignedMessageResolver() {
                @Override
                public String content() {
                    return text;
                }

                @Override
                public CompletableFuture<SignedMessage> resolveSignedMessage(String argumentName,
                                                                             com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
                    return CompletableFuture.completedFuture(SignedMessage.system(text, Component.text(text)));
                }
            };
        };
    }

    /**
     * Reads one JSON or SNBT value where the vanilla readers would end it: a quoted string, a value in balanced braces
     * or brackets whose quoted strings may hold any character, or else everything up to the next whitespace.
     */
    private static String readValue(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        if (!reader.canRead()
                || (reader.peek() != '{' && reader.peek() != '[' && !StringReader.isQuotedStringStart(reader.peek()))) {
            return readUntilWhitespace(reader);
        }
        int depth = 0;
        char quote = 0;
        while (reader.canRead()) {
            char c = reader.read();
            if (quote != 0) {
                if (c == '\\' && reader.canRead()) {
                    reader.skip();
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (StringReader.isQuotedStringStart(c)) {
                quote = c;
            } else if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
            }
            if (quote == 0 && depth == 0) {
                return reader.getString()
                             .substring(start, reader.getCursor());
            }
        }
        reader.setCursor(start);
        throw syntaxError("Unterminated value", reader);
    }

    private static CommandSyntaxException syntaxError(String message, StringReader reader) {
        return new SimpleCommandExceptionType(new LiteralMessage(message)).createWithContext(reader);
    }

    /**
     * Reads a name or a selector without arguments, rejecting while parsing the selectors that cannot match the
     * argument, as a server does: {@code @a} and {@code @e} for a single target, and {@code @e} for players.
     */
    private static <R> ArgumentType<R> selector(boolean single, boolean playersOnly, Function<String, R> resolver) {
        return reader -> {
            int start = reader.getCursor();
            String token = readUntilWhitespace(reader);
            if (token.startsWith("@") && token.contains("[")) {
                throw new UnsupportedOperationException("Selector arguments are not supported by CommandTester: "
                        + token);
            }
            String error = null;
            if (single && (token.equals("@a") || token.equals("@e"))) {
                error = playersOnly ? "Only one player is allowed, but the provided selector allows more than one"
                        : "Only one entity is allowed, but the provided selector allows more than one";
            } else if (playersOnly && token.equals("@e")) {
                error = "Only players may be affected by this command, but the provided selector includes entities";
            }
            if (error != null) {
                reader.setCursor(start);
                throw syntaxError(error, reader);
            }
            return resolver.apply(token);
        };
    }

    /**
     * Resolves a name, {@code @s} (the executing entity), {@code @p} (the sender when it is a player, otherwise the
     * first registered player), {@code @r} (the first registered player), {@code @a} (the registered players) or
     * {@code @e} (the registered entities). As on a server, an executing player is online, so {@code @a}, {@code @e},
     * {@code @r} and its name find it even when it is not registered. A name that was never registered throws, since in
     * a test it almost always means a missing fixture.
     */
    private static List<Entity> select(String token, CommandSourceStack source) {
        CommandTester tester = requireCurrent();
        CommandSender sender = source.getSender();
        List<Entity> players = new ArrayList<>(tester.fakePlayers.values());
        List<Entity> entities = new ArrayList<>(tester.fakeEntities.values());
        if (sender instanceof Player && !entities.contains(sender)) {
            players.add((Player) sender);
            entities.add((Player) sender);
        }
        switch (token) {
            case "@s":
                Entity executor = source.getExecutor();
                return executor == null ? List.of() : List.of(executor);
            case "@p":
                // Nearest to the source, which is the executing player itself.
                if (sender instanceof Player) {
                    return List.of((Player) sender);
                }
                // fall through
            case "@r":
                return players.stream()
                              .limit(1)
                              .collect(Collectors.toList());
            case "@a":
                return players;
            case "@e":
                return entities;
            default:
                Entity entity = tester.fakeEntities.get(token.toLowerCase(Locale.ROOT));
                if (entity == null && sender instanceof Player && sender.getName()
                                                                        .equalsIgnoreCase(token)) {
                    entity = (Player) sender;
                }
                if (entity == null) {
                    throw new IllegalArgumentException("No fake entity registered with name: " + token
                            + ". Call withFakePlayer() or withFakeEntity() before execute().");
                }
                return List.of(entity);
        }
    }

    private static List<Player> players(List<Entity> entities) {
        return entities.stream()
                       .filter(Player.class::isInstance)
                       .map(Player.class::cast)
                       .collect(Collectors.toList());
    }

    @Nullable
    private Object resourceValue(RegistryKey<?> key, String token) {
        CommandTester tester = requireCurrent();
        Map<String, Object> values = tester.fakeRegistryValues.get(key);
        NamespacedKey valueKey = NamespacedKey.fromString(token);
        // A server rejects a key its registry lacks while parsing; word() turns null into that error.
        return values == null || valueKey == null ? null : values.get(valueKey.toString());
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

    // Vanilla's game mode argument takes the lower-case names only.
    @Nullable
    private static GameMode gameMode(String token) {
        for (GameMode mode : GameMode.values()) {
            if (mode.name()
                    .toLowerCase(Locale.ROOT)
                    .equals(token)) {
                return mode;
            }
        }
        return null;
    }

    // Vanilla's ChatFormatting.getByName ignores case and characters other than letters.
    private static String colorKey(String name) {
        return name.toLowerCase(Locale.ROOT)
                   .replaceAll("[^a-z]", "");
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
                throw new SimpleCommandExceptionType(new LiteralMessage("Unknown value '" + token
                        + "'")).createWithContext(reader);
            }
            return value;
        };
    }

    /**
     * Reads a vanilla range such as {@code 1..10}, {@code ..5}, {@code 3..} or {@code 5}, and rejects {@code ..}, a
     * bound that is not a number, and a minimum above the maximum while parsing like a server.
     */
    private static <T extends Comparable<T>, P> ArgumentType<P> range(Function<String, T> parseNumber,
                                                                      Function<Range<T>, P> provider) {
        return reader -> {
            int start = reader.getCursor();
            String token = readUntilWhitespace(reader);
            int separator = token.indexOf("..");
            String min = separator < 0 ? token : token.substring(0, separator);
            String max = separator < 0 ? token : token.substring(separator + 2);
            String error;
            try {
                // Vanilla's MinMaxBounds reads only these characters into a bound, so 1e3, NaN or +5 are not numbers.
                if (!min.matches("[0-9.-]*") || !max.matches("[0-9.-]*")) {
                    throw new NumberFormatException();
                }
                T lower = min.isEmpty() ? null : parseNumber.apply(min);
                T upper = max.isEmpty() ? null : parseNumber.apply(max);
                if (lower == null && upper == null) {
                    error = "Expected value or range of values";
                } else if (lower != null && upper != null && lower.compareTo(upper) > 0) {
                    error = "Min cannot be bigger than max";
                } else if (lower == null) {
                    return provider.apply(Range.atMost(Objects.requireNonNull(upper)));
                } else {
                    return provider.apply(upper == null ? Range.atLeast(lower) : Range.closed(lower, upper));
                }
            } catch (NumberFormatException e) {
                error = "Invalid number '" + token + "'";
            }
            reader.setCursor(start);
            throw new SimpleCommandExceptionType(new LiteralMessage(error)).createWithContext(reader);
        };
    }

    /**
     * Reads a vanilla time such as {@code 20}, {@code 10s} or {@code 0.5d} into ticks, rejecting an unknown unit and
     * fewer ticks than {@code minimum} while parsing, and suggests the units after a number, like a server.
     */
    private static ArgumentType<Integer> time(int minimum) {
        // No unit means ticks.
        Map<String, Integer> units = Map.of("d", 24000, "s", 20, "t", 1, "", 1);
        return new ArgumentType<>() {
            @Override
            public Integer parse(StringReader reader) throws CommandSyntaxException {
                float amount = reader.readFloat();
                int ticksPerUnit = units.getOrDefault(reader.readUnquotedString(), 0);
                String error;
                if (ticksPerUnit == 0) {
                    error = "Invalid unit";
                } else {
                    int ticks = Math.round(amount * ticksPerUnit);
                    if (ticks >= minimum) {
                        return ticks;
                    }
                    error = "Tick count must not be less than " + minimum + ", found " + ticks;
                }
                throw new SimpleCommandExceptionType(new LiteralMessage(error)).createWithContext(reader);
            }

            @Override
            public <S> CompletableFuture<Suggestions> listSuggestions(com.mojang.brigadier.context.CommandContext<S> context,
                                                                      SuggestionsBuilder builder) {
                StringReader reader = new StringReader(builder.getRemaining());
                try {
                    reader.readFloat();
                } catch (CommandSyntaxException e) {
                    return builder.buildFuture();
                }
                SuggestionsBuilder unitBuilder = builder.createOffset(builder.getStart() + reader.getCursor());
                units.keySet()
                     .stream()
                     .filter(unit -> !unit.isEmpty() && unit.startsWith(unitBuilder.getRemaining()))
                     .forEach(unitBuilder::suggest);
                return unitBuilder.buildFuture();
            }
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

    /**
     * Stubs {@code ArgumentTypes.blockInWorldPredicate()}, which Paper added in 1.21.8, when the Paper API on the
     * classpath has it. The predicate matches blocks of the material of the id.
     */
    private static void stubBlockInWorldPredicate(MockedStatic<ArgumentTypes> argumentTypes) {
        Method method;
        Class<?> predicateClass;
        try {
            method = ArgumentTypes.class.getMethod("blockInWorldPredicate");
            predicateClass = Class.forName("io.papermc.paper.command.brigadier.argument.predicate.BlockInWorldPredicate");
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            return;
        }
        argumentTypes.when(() -> method.invoke(null))
                     .thenReturn(material("block", material -> Mockito.mock(predicateClass, invocation -> {
                         if (!invocation.getMethod()
                                        .getName()
                                        .equals("testBlock")) {
                             return Mockito.RETURNS_DEFAULTS.answer(invocation);
                         }
                         Block block = invocation.getArgument(0);
                         return resultOf(invocation.getMethod()
                                                   .getReturnType(),
                                         block.getType() == material);
                     })));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object resultOf(Class<?> resultClass, boolean matches) {
        return Enum.valueOf((Class) resultClass, matches ? "TRUE" : "FALSE");
    }

    /**
     * Reads a material id such as {@code stone} or {@code minecraft:stone}, rejects an unknown one while parsing like a
     * server, and suggests the ids that start with the input. It does not tell items from blocks, since
     * {@link Material#isItem()} and {@link Material#isBlock()} need a server from Paper 1.21.3.
     */
    private static <T> ArgumentType<T> material(String kind, Function<Material, T> value) {
        return new ArgumentType<T>() {
            @Override
            public T parse(StringReader reader) throws CommandSyntaxException {
                int start = reader.getCursor();
                String token = readUntilWhitespace(reader);
                Material material = Material.matchMaterial(token);
                if (material == null) {
                    reader.setCursor(start);
                    throw new SimpleCommandExceptionType(new LiteralMessage("Unknown " + kind + " '" + token
                            + "'")).createWithContext(reader);
                }
                return value.apply(material);
            }

            @Override
            public <S> CompletableFuture<Suggestions> listSuggestions(com.mojang.brigadier.context.CommandContext<S> context,
                                                                      SuggestionsBuilder builder) {
                String input = builder.getRemaining();
                for (Material material : Material.values()) {
                    if (material.isLegacy()) {
                        continue;
                    }
                    String key = material.getKey()
                                         .toString();
                    if (key.startsWith(input) || key.startsWith("minecraft:" + input)) {
                        builder.suggest(key);
                    }
                }
                return builder.buildFuture();
            }
        };
    }

    private static ArgumentType<BlockPositionResolver> blockPosition() {
        return reader -> {
            int x = reader.readInt();
            reader.skipWhitespace();
            int y = reader.readInt();
            reader.skipWhitespace();
            int z = reader.readInt();
            return source -> Position.block(x, y, z);
        };
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

        private Collection<? extends Command> createCommands() {
            List<Command> commands = new ArrayList<>();
            for (Supplier<? extends Command> commandSupplier : commandSuppliers) {
                commands.add(commandSupplier.get());
            }
            defaults.applyTo(commands);
            return commands;
        }
    }
}
