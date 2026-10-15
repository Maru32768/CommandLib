package dev.marulabs.commandlib;

import dev.marulabs.commandlib.annotation.*;
import dev.marulabs.commandlib.annotation.Optional;
import dev.marulabs.commandlib.argument.CommonEnumArgument;
import dev.marulabs.commandlib.argument.CommonIntegerArgument;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The common tests are compiled without -parameters, so the arguments are named with @Name.
class AnnotationParserTest {
    private static AnnotationParser<TestCommandContext, TestCommand> parser() {
        return new AnnotationParser<>(TestCommandContext.class,
                                      TestCommand::new).registerArgument(String.class,
                                                                         p -> p.annotation(Greedy.class) == null
                                                                                 ? new StrArg(p.name())
                                                                                 : new StrArg(p.name(),
                                                                                              CommonStringArgument.Type.PHRASE))
                                                       .registerArgument(Integer.class, p -> {
                                                           Range range = p.annotation(Range.class);
                                                           return range == null ? new IntArg(p.name())
                                                                   : new IntArg(p.name(),
                                                                                (int) range.min(),
                                                                                (int) range.max());
                                                       })
                                                       .registerArgument(Enum.class, AnnotationParserTest::enumArgument)
                                                       .registerContextValue(CommandActor.class,
                                                                             CommonCommandContext::getActor);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static CommonArgument<?, TestCommandContext, ?> enumArgument(AnnotatedParameter parameter) {
        return new EnumArg(parameter.name(), parameter.type());
    }

    private static final class IntArg extends CommonIntegerArgument<TestCommandContext, IntArg> {
        IntArg(String name) {
            super(name);
        }

        IntArg(String name, int min, int max) {
            super(name, min, max);
        }
    }

    private static final class StrArg extends CommonStringArgument<TestCommandContext, StrArg> {
        StrArg(String name) {
            super(name);
        }

        StrArg(String name, Type type) {
            super(name, type);
        }
    }

    private static final class EnumArg<E extends Enum<E>> extends CommonEnumArgument<E, TestCommandContext, EnumArg<E>> {
        EnumArg(String name, Class<E> type) {
            super(name, type);
        }
    }

    enum Mode {
        SURVIVAL,
        CREATIVE
    }

    @Command(value = "game", aliases = "g")
    @Description("Manage games")
    static final class AnnotatedGame {
        @Subcommand("start")
        @Description("Start a game")
        @Permission(defaultPermission = DefaultPermission.ALL, description = "Start games")
        void start(TestCommandContext ctx,
                   @Flag(value = "force", shortName = 'f') @Description("Skip checks") boolean force,
                   @Option(value = "limit", shortName = 'n') @Range(min = 1, max = 10) @Default("4") int limit,
                   @Name("arena") String arena,
                   @Name("mode") @Default("creative") Mode mode) {
            ctx.sendMessage("start " + arena + " " + mode + " " + limit + " " + force);
        }

        @Subcommand("stop")
        void stop(TestCommandContext ctx, @Flag(value = "verbose", shortName = 'v', max = 3) int verbose) {
            ctx.sendMessage("stop " + verbose);
        }

        @Subcommand("kick")
        void kick(TestCommandContext ctx,
                  @Name("reason") @Suggest({"afk", "spam"}) String reason,
                  @Name("players") @Optional List<String> players) {
            ctx.sendMessage("kick " + players + " " + reason);
        }

        @Execute
        @Description("Show a game")
        void show(TestCommandContext ctx, @Name("id") @Range(min = 0, max = 99) int id, CommandActor actor) {
            ctx.sendMessage("show " + id + " to " + actor.getName());
        }
    }

    private static TestCommand fluentGame() {
        return new TestCommand("game") {{
            addAliases("g");
            description("Manage games");
            argument(new IntArg("id", 0, 99)).description("Show a game")
                                             .execute((id,
                                                       ctx) -> ctx.sendMessage("show " + id + " to "
                                                               + ctx.getActor()
                                                                    .getName()));
            addChildren(new TestCommand("kick") {{
                argument(new StrArg("reason").setSuggestionProvider(sb -> {
                    sb.suggest("afk");
                    sb.suggest("spam");
                }),
                         new StrArg("players").variadic()
                                              .optional(List.of())).execute((reason, players, ctx) -> ctx.sendMessage("kick " + players + " " + reason));
            }}, new TestCommand("start") {{
                description("Start a game");
                permission(DefaultPermission.ALL, "Start games");
                CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f')
                                                                                 .description("Skip checks"));
                CommandOption<Integer, TestCommandContext> limit = option(Options.integer("limit",
                                                                                          'n',
                                                                                          4,
                                                                                          1,
                                                                                          10));
                argument(new StrArg("arena"),
                         new EnumArg<>("mode", Mode.class).optional(Mode.CREATIVE))
                                                                                   .execute((arena,
                                                                                             mode,
                                                                                             ctx) -> ctx.sendMessage("start "
                                                                                                     + arena
                                                                                                     + " "
                                                                                                     + mode
                                                                                                     + " "
                                                                                                     + ctx.getOption(limit)
                                                                                                     + " "
                                                                                                     + ctx.getOption(force)));
            }}, new TestCommand("stop") {{
                CommandOption<Integer, TestCommandContext> verbose = option(Options.count("verbose", 'v', 3));
                execute(ctx -> ctx.sendMessage("stop " + ctx.getOption(verbose)));
            }});
        }};
    }

    @Test
    void annotated_command_matches_the_fluent_command() throws Exception {
        TestCommand annotated = parser().parse(new AnnotatedGame());
        TestCommand fluent = fluentGame();

        assertThat(CommandTreeRenderer.render(List.of(annotated),
                                              "test.command")).isEqualTo(CommandTreeRenderer.render(List.of(fluent),
                                                                                                    "test.command"));
        assertThat(CommandDocs.of("test.command", annotated)
                              .json()).isEqualTo(CommandDocs.of("test.command", fluent)
                                                            .json());
        List<String> inputs = List.of("game",
                                      "g 7",
                                      "game start",
                                      "game start lobby",
                                      "game start -f -n 9 lobby survival",
                                      "game stop -vv",
                                      "game kick afk",
                                      "game kick spam Steve Alex");
        assertThat(transcript(annotated, inputs)).isEqualTo(transcript(fluent, inputs))
                                                 .contains("game start lobby -> start lobby CREATIVE 4 false",
                                                           "game start -f -n 9 lobby survival -> start lobby SURVIVAL 9 true",
                                                           "game stop -vv -> stop 2",
                                                           "game kick spam Steve Alex -> kick [Steve, Alex] spam",
                                                           "g 7 -> show 7 to test");
        assertThat(new TestCommandRunner(annotated).suggest("game kick ")).isEqualTo(new TestCommandRunner(fluent).suggest("game kick "))
                                                                          .contains("afk", "spam");
    }

    private static List<String> transcript(TestCommand command, List<String> inputs) throws Exception {
        TestCommandRunner runner = new TestCommandRunner(command);
        List<String> lines = new ArrayList<>();
        for (String input : inputs) {
            lines.add(input + " -> " + String.join(" | ",
                                                   runner.execute(input)
                                                         .messages()));
        }
        return lines;
    }

    @Command("calc")
    static final class AsyncCommand {
        @Subcommand("later")
        CompletionStage<?> later(TestCommandContext ctx) {
            return CompletableFuture.completedFuture(null)
                                    .thenRun(() -> ctx.setResult(5));
        }

        @Subcommand("result")
        int result(@Name("n") int n) {
            return n * 2;
        }

        @Subcommand("fail")
        void fail(TestCommandContext ctx) throws IOException {
            if (!ctx.getInputs()
                    .isEmpty()) {
                throw new IOException("disk");
            }
        }

        @Subcommand("deny")
        void deny(TestCommandContext ctx) throws CommandPrerequisiteException {
            if (!ctx.getInputs()
                    .isEmpty()) {
                throw new CommandPrerequisiteException("Denied.");
            }
        }
    }

    @Test
    void return_values_and_exceptions_map_to_the_execution() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(parser().parse(new AsyncCommand()));

        assertThat(runner.executeAndGetResult("calc later")).isEqualTo(5);
        assertThat(runner.executeAndGetResult("calc result 21")).isEqualTo(42);
        assertThat(runner.execute("calc deny")
                         .messages()).containsExactly("Denied.");
        assertThat(runner.executeAndGetResult("calc fail")).isZero();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Upper {
    }

    @Command("say")
    static final class UpperCommand {
        @Execute
        void say(TestCommandContext ctx, @Name("text") @Upper @Greedy String text) {
            ctx.sendMessage(text);
        }
    }

    @Test
    void custom_annotation_handler_configures_the_argument() throws Exception {
        TestCommand command = parser().registerAnnotation(Upper.class,
                                                          String.class,
                                                          (upper,
                                                           argument) -> argument.transformer(x -> x.toUpperCase(Locale.ROOT)))
                                      .parse(new UpperCommand());

        assertThat(new TestCommandRunner(command).execute("say hello world")
                                                 .messages()).containsExactly("HELLO WORLD");
        assertThatThrownBy(() -> parser().registerAnnotation(Upper.class, Integer.class, (upper, argument) -> argument)
                                         .parse(new UpperCommand())).hasMessageContaining("@Upper, which applies to java.lang.Integer");
    }

    static final class NotACommand {
    }

    @Command("x")
    static final class UnnamedParameter {
        @Execute
        void run(String text) {
        }
    }

    @Command("x")
    static final class UnknownType {
        @Execute
        void run(@Name("value") Thread value) {
        }
    }

    @Command("x")
    static final class RangeOnString {
        @Execute
        void run(@Name("value") @Range(max = 3) String value) {
        }
    }

    @Command("x")
    static final class BadDefault {
        @Execute
        void run(@Name("value") @Default("ten") int value) {
        }
    }

    @Command("x")
    static final class DescribedExecutor {
        @Execute
        @Description("Nothing")
        void run() {
        }
    }

    @Command("x")
    static final class TwiceConfigured {
        @Subcommand("a")
        @Description("One")
        void one() {
        }

        @Subcommand("a")
        @Description("Two")
        void two(@Name("n") int n) {
        }
    }

    @Command("x")
    static final class BadReturnType {
        @Execute
        String run() {
            return "";
        }
    }

    @Command("x")
    static final class DifferentOptions {
        @Subcommand("a")
        void one(@Flag("force") boolean force) {
        }

        @Subcommand("a")
        void two(@Flag(value = "force", shortName = 'f') boolean force, @Name("n") int n) {
        }
    }

    @Command("x")
    static final class MissingSender {
        @Execute
        void run(@Sender Thread thread) {
        }
    }

    @Command("x")
    static final class UnmarkedMethod {
        @Description("Forgot @Subcommand")
        void run() {
        }
    }

    @Test
    void invalid_signatures_fail_with_the_method_and_the_parameter() {
        AnnotationParser<TestCommandContext, TestCommand> parser = parser();

        assertThatThrownBy(() -> parser.parse(new NotACommand())).hasMessageEndingWith("NotACommand is not annotated with @Command");
        assertThatThrownBy(() -> parser.parse(new UnnamedParameter())).hasMessage("UnnamedParameter#run(String): parameter arg0 has no name; compile with -parameters or annotate it with @Name");
        assertThatThrownBy(() -> parser.parse(new UnknownType())).hasMessageContaining("has no argument type for java.lang.Thread");
        assertThatThrownBy(() -> parser.parse(new RangeOnString())).hasMessageContaining("has @Range, but java.lang.String is not a number");
        assertThatThrownBy(() -> parser.parse(new BadDefault())).hasMessageContaining("has @Default(\"ten\"), which is not a valid Integer");
        assertThatThrownBy(() -> parser.parse(new DescribedExecutor())).hasMessageContaining("put them on the class");
        assertThatThrownBy(() -> parser.parse(new TwiceConfigured())).hasMessageContaining("which TwiceConfigured#one() already configures");
        assertThatThrownBy(() -> parser.parse(new BadReturnType())).hasMessageContaining("must return void, int or a CompletionStage");
        assertThatThrownBy(() -> parser.parse(new DifferentOptions())).hasMessageContaining("declares the option --force differently");
        assertThatThrownBy(() -> parser.parse(new MissingSender())).hasMessageContaining("has no context value for java.lang.Thread");
        assertThatThrownBy(() -> parser.parse(new UnmarkedMethod())).hasMessageContaining("has @Description but neither @Execute nor @Subcommand");
    }

    static final class GamePlayer {
        private final String name;

        GamePlayer(String name) {
            this.name = name;
        }
    }

    static final class Economy {
        int balance(String name) {
            return name.length();
        }
    }

    static final class Arena {
        private final String name;

        Arena(String name) {
            this.name = name;
        }
    }

    abstract static class EconomyCommand {
        @Dependency
        Economy economy;
    }

    @Command("game")
    static final class GameCommand extends EconomyCommand {
        @Dependency
        private GamePlayer admin;

        @Subcommand("me")
        void me(TestCommandContext ctx, GamePlayer player) {
            ctx.sendMessage(player.name + " has " + economy.balance(player.name) + " admin " + admin.name);
        }

        @Subcommand("sender")
        void sender(TestCommandContext ctx, @Sender GamePlayer player, Economy economy) {
            ctx.sendMessage(player.name + " " + (economy == this.economy));
        }

        @Subcommand("arena")
        void arena(TestCommandContext ctx, Arena arena) {
            ctx.sendMessage("in " + arena.name);
        }
    }

    @Test
    void parameters_and_fields_read_context_values_providers_and_dependencies() throws Exception {
        List<String> mapped = new ArrayList<>();
        Economy economy = new Economy();
        TestCommand command = parser().registerDependency(Economy.class, economy)
                                      // The field gets the dependency; the parameters get the sender mapped below.
                                      .registerDependency(GamePlayer.class, new GamePlayer("root"))
                                      .registerContextValue(GamePlayer.class, ctx -> {
                                          mapped.add(ctx.getActor()
                                                        .getName());
                                          return new GamePlayer(ctx.getActor()
                                                                   .getName());
                                      })
                                      .registerContextValue(Arena.class)
                                      .parse(new GameCommand());
        // The parsed command provides the values to fluent code, and a middleware can put the declared ones.
        command.addMiddleware((ctx, next) -> {
            mapped.add("middleware " + ctx.get(GamePlayer.class).name);
            ctx.put(Arena.class, new Arena("lobby"));
            return next.proceed();
        });
        command.addChildren(new TestCommand("fluent") {{
            execute(ctx -> ctx.sendMessage("fluent " + ctx.get(GamePlayer.class).name + " " + ctx.get(Economy.class)
                                                                                                 .balance("abc")));
        }});
        TestCommandRunner runner = new TestCommandRunner(command);

        assertThat(runner.execute("game me")
                         .messages()).containsExactly("test has 4 admin root");
        assertThat(runner.execute("game sender")
                         .messages()).containsExactly("test true");
        assertThat(runner.execute("game arena")
                         .messages()).containsExactly("in lobby");
        assertThat(runner.execute("game fluent")
                         .messages()).containsExactly("fluent test 3");
        // The sender is mapped once per execution.
        assertThat(mapped).containsExactly("test",
                                           "middleware test",
                                           "test",
                                           "middleware test",
                                           "test",
                                           "middleware test",
                                           "test",
                                           "middleware test");
    }

    @Test
    void a_declared_context_value_nobody_gives_fails_the_execution() throws Exception {
        TestCommand command = parser().registerContextValue(Arena.class)
                                      .registerDependency(Economy.class, new Economy())
                                      .registerDependency(GamePlayer.class, new GamePlayer("root"))
                                      .registerContextValue(GamePlayer.class, ctx -> new GamePlayer("x"))
                                      .parse(new GameCommand());

        assertThat(new TestCommandRunner(command).execute("game arena")
                                                 .messages()).containsExactly("command.failed",
                                                                              "Check the console for details.");
    }

    @Command("x")
    static final class FinalDependency {
        @Dependency
        final Economy economy = new Economy();
    }

    @Test
    void dependencies_must_be_registered_and_settable() {
        assertThatThrownBy(() -> parser().parse(new GameCommand())).hasMessage("GameCommand.admin has @Dependency, but no dependency of type "
                + GamePlayer.class.getName() + " is registered; register one with registerDependency");
        assertThatThrownBy(() -> parser().registerDependency(Economy.class, new Economy())
                                         .parse(new FinalDependency())).hasMessage("FinalDependency.economy has @Dependency, but is static or final");
    }
}
