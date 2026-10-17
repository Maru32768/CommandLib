package dev.marulabs.commandlib;

import dev.marulabs.commandlib.annotation.*;
import dev.marulabs.commandlib.annotation.Optional;
import dev.marulabs.commandlib.argument.CommonEnumArgument;
import dev.marulabs.commandlib.argument.CommonIntegerArgument;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import dev.marulabs.commandlib.command.CommandMiddleware;
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
        // The IOException is unexpected: the sender is told to check the console.
        assertThat(runner.execute("calc fail")
                         .messages()).endsWith("Check the console for details.");
    }

    @Command("arena")
    @Condition("open")
    static final class ConditionCommand {
        @Subcommand("join")
        void join(TestCommandContext ctx) {
            ctx.sendMessage("joined");
        }

        @Subcommand("start")
        @Condition({"open", "staff"})
        void start(TestCommandContext ctx) {
            ctx.sendMessage("started");
        }
    }

    @Command("arena")
    static final class UnknownCondition {
        @Subcommand("join")
        @Condition("closed")
        void join() {
        }
    }

    @Test
    void conditions_add_the_registered_prerequisites() throws Exception {
        boolean[] open = {false};
        TestCommand command = parser().registerCondition("open", ctx -> {
            if (!open[0]) {
                throw new CommandPrerequisiteException("The arena is closed.");
            }
        })
                                      .registerCondition("staff", ctx -> {
                                          throw new CommandPrerequisiteException("Staff only.");
                                      })
                                      .parse(new ConditionCommand());
        TestCommandRunner runner = new TestCommandRunner(command);

        assertThat(runner.execute("arena join")
                         .messages()).containsExactly("The arena is closed.");
        open[0] = true;
        assertThat(runner.execute("arena join")
                         .messages()).containsExactly("joined");
        assertThat(runner.execute("arena start")
                         .messages()).containsExactly("Staff only.");
        assertThatThrownBy(() -> parser().parse(new UnknownCondition())).hasMessageContaining("@Condition(\"closed\")");
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

    @Command("x")
    static final class UpperFlag {
        @Execute
        void run(@Flag("loud") @Upper boolean loud) {
        }
    }

    @Command("x")
    static final class UpperSender {
        @Execute
        void run(@Sender @Upper CommandActor actor) {
        }
    }

    @Test
    void custom_annotation_on_a_parameter_that_is_no_argument_is_rejected() {
        AnnotationParser<TestCommandContext, TestCommand> parser = parser().registerAnnotation(Upper.class,
                                                                                               Object.class,
                                                                                               (upper,
                                                                                                argument) -> argument);

        assertThatThrownBy(() -> parser.parse(new UpperFlag())).hasMessageContaining("parameter loud has @Upper, which does not apply to a @Flag parameter");
        assertThatThrownBy(() -> parser.parse(new UpperSender())).hasMessageContaining("has @Upper, which does not apply to a @Sender parameter");
    }

    abstract static class BaseCommand {
        @Execute
        void run(TestCommandContext ctx, @Name("name") @Suggest(method = "names") String name) {
            ctx.sendMessage("base " + name);
        }

        @Subcommand("stop")
        void stop(TestCommandContext ctx) {
            ctx.sendMessage("base stop");
        }

        List<String> names() {
            return List.of("alex", "steve");
        }
    }

    @Command("x")
    static final class DerivedCommand extends BaseCommand {
        @Override
        void stop(TestCommandContext ctx) {
            ctx.sendMessage("derived stop");
        }
    }

    @Test
    void annotated_methods_of_superclasses_are_read_and_an_override_runs_once() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(parser().parse(new DerivedCommand()));

        assertThat(runner.execute("x alex")
                         .messages()).containsExactly("base alex");
        assertThat(runner.execute("x stop")
                         .messages()).containsExactly("derived stop");
        assertThat(runner.suggest("x ")).contains("alex", "steve", "stop");
    }

    abstract static class GenericBase<P> {
        @Execute
        void run(TestCommandContext ctx, @Name("p") P p) {
            ctx.sendMessage("base " + p);
        }
    }

    @Command("x")
    static final class GenericOverride extends GenericBase<String> {
        @Override
        @Execute
        void run(TestCommandContext ctx, @Name("p") String p) {
            ctx.sendMessage("override " + p);
        }
    }

    @Command("x")
    static final class UnannotatedGenericOverride extends GenericBase<String> {
        @Override
        void run(TestCommandContext ctx, String p) {
        }
    }

    // The bridge javac adds to the subclass stands for the superclass declaration of the erased parameter types.
    @Test
    void an_override_of_a_generic_method_is_read_once() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(parser().parse(new GenericOverride()));

        assertThat(runner.execute("x a")
                         .messages()).containsExactly("override a");
        assertThatThrownBy(() -> parser().parse(new UnannotatedGenericOverride())).hasMessageContaining("GenericBase#run(TestCommandContext, Object) is overridden by UnannotatedGenericOverride#run(TestCommandContext, String)");
    }

    @Description("Base")
    @Permission("base.node")
    abstract static class DescribedBase {
        @Execute
        void run(TestCommandContext ctx) {
        }
    }

    @Command("x")
    @Description("Derived")
    static final class DescribedDerived extends DescribedBase {
    }

    @Test
    void class_annotations_of_superclasses_apply_unless_the_class_gives_its_own() {
        TestCommand command = parser().parse(new DescribedDerived());

        assertThat(command.staticDescription()).isEqualTo("Derived");
        assertThat(command.permissionName("test.command")).isEqualTo("base.node");
    }

    @Command("x")
    static final class WildcardList {
        @Execute
        void run(TestCommandContext ctx, @Name("names") List<? extends String> names) {
            ctx.sendMessage("names " + names);
        }
    }

    // Kotlin compiles a List<Player> parameter to List<? extends Player>.
    @Test
    void a_list_of_a_wildcard_takes_its_upper_bound() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(parser().parse(new WildcardList()));

        assertThat(runner.execute("x a b")
                         .messages()).containsExactly("names [a, b]");
    }

    interface Pingable {
        @Subcommand("ping")
        default void ping(TestCommandContext ctx) {
            ctx.sendMessage("pong");
        }

        @Execute
        void run(TestCommandContext ctx);
    }

    @Command("x")
    static final class PingCommand implements Pingable {
        @Override
        public void run(TestCommandContext ctx) {
            ctx.sendMessage("run");
        }
    }

    @Test
    void annotated_methods_of_interfaces_are_read() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(parser().parse(new PingCommand()));

        assertThat(runner.execute("x ping")
                         .messages()).containsExactly("pong");
        assertThat(runner.execute("x")
                         .messages()).containsExactly("run");
    }

    @Command("x")
    static final class FractionalRange {
        @Execute
        void run(@Name("count") @Range(min = 1.5) int count) {
        }
    }

    @Command("x")
    static final class InvertedRange {
        @Execute
        void run(@Name("ratio") @Range(min = 2, max = 1) int ratio) {
        }
    }

    @Command("x")
    static final class SpacedPlaceholder {
        @Subcommand("<a b> c")
        void run(@Name("a") String a) {
        }
    }

    @Command("x")
    static final class EmptyPlaceholder {
        @Subcommand("<> c")
        void run() {
        }
    }

    @Command("x")
    static final class RequiredOptionWithDefault {
        @Execute
        void run(@Option(value = "n", required = true) @Default("1") int n) {
        }
    }

    @Command("x")
    static final class DefaultOutsideRange {
        @Execute
        void run(@Name("n") @Range(min = 1, max = 10) @Default("0") int n) {
        }
    }

    @Command("x")
    static final class OptionalOutsideRange {
        @Execute
        void run(@Name("n") @Range(min = 1, max = 10) @Optional int n) {
        }
    }

    @Command("x")
    static final class OptionOutsideRange {
        @Execute
        void run(@Option("n") @Range(min = 1, max = 10) int n) {
        }
    }

    @Command("x")
    @dev.marulabs.commandlib.annotation.Cooldown(0)
    static final class ZeroCooldown {
        @Execute
        void run() {
        }
    }

    @Command("x")
    static final class ZeroVariadic {
        @Execute
        void run(@Name("names") @Variadic(max = 0) List<String> names) {
        }
    }

    // The builder methods do not know the annotation their value comes from.
    @Test
    void builder_errors_name_the_class_or_the_method() {
        assertThatThrownBy(() -> parser().parse(new ZeroCooldown())).hasMessageStartingWith("class ZeroCooldown: duration must be positive");
        assertThatThrownBy(() -> parser().parse(new ZeroVariadic())).hasMessageStartingWith("ZeroVariadic#run(List): maxCount must be at least 1");
    }

    @Test
    void contradictory_or_malformed_annotations_are_rejected() {
        AnnotationParser<TestCommandContext, TestCommand> parser = parser();

        assertThatThrownBy(() -> parser.parse(new FractionalRange())).hasMessageContaining("parameter count has @Range with a fractional bound");
        assertThatThrownBy(() -> parser.parse(new InvertedRange())).hasMessageContaining("parameter ratio has @Range with min 2.0 above max 1.0");
        assertThatThrownBy(() -> parser.parse(new SpacedPlaceholder())).hasMessageContaining("has '<a' in its @Subcommand path");
        assertThatThrownBy(() -> parser.parse(new EmptyPlaceholder())).hasMessageContaining("has '<>' in its @Subcommand path");
        assertThatThrownBy(() -> parser.parse(new RequiredOptionWithDefault())).hasMessageContaining("is a required option and cannot take @Default or @Optional");
        assertThatThrownBy(() -> parser.parse(new DefaultOutsideRange())).hasMessageContaining("parameter n defaults to 0, outside its @Range");
        assertThatThrownBy(() -> parser.parse(new OptionalOutsideRange())).hasMessageContaining("parameter n defaults to 0, outside its @Range");
        assertThatThrownBy(() -> parser.parse(new OptionOutsideRange())).hasMessageContaining("parameter n defaults to 0, outside its @Range");
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

    @Command("export")
    @Examples("/export -j data")
    @MutuallyExclusive({"json", "yaml"})
    static final class AnnotatedExport {
        @Execute
        void export(TestCommandContext ctx,
                    @Flag(value = "force", shortName = 'f') boolean force,
                    @Option(value = "reason", shortName = 'r') @Requires("force") @Permission(defaultPermission = DefaultPermission.OP) String reason,
                    @Option(value = "mode", shortName = 'm') @Default("survival") Mode mode,
                    @Option("threads") @Requires(value = "mode", equalTo = "creative") @Default("1") int threads,
                    @Flag(value = "json", shortName = 'j') boolean json,
                    @Flag(value = "yaml", shortName = 'y') boolean yaml,
                    @Name("file") String file) {
            ctx.sendMessage(file + " " + force + " " + reason + " " + mode + " " + threads + " " + json + " " + yaml);
        }
    }

    private static TestCommand fluentExport() {
        return new TestCommand("export") {{
            examples("/export -j data");
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<String, TestCommandContext> reason = option(Options.<TestCommandContext>string("reason",
                                                                                                         'r',
                                                                                                         "")
                                                                             .permission(DefaultPermission.OP)
                                                                             .requires(force));
            CommandOption<Mode, TestCommandContext> mode = option(Options.of("mode",
                                                                             'm',
                                                                             new EnumArg<>("mode",
                                                                                           Mode.class).optional(Mode.SURVIVAL)));
            CommandOption<Integer, TestCommandContext> threads = option(Options.<TestCommandContext>integer("threads",
                                                                                                            1,
                                                                                                            Integer.MIN_VALUE,
                                                                                                            Integer.MAX_VALUE)
                                                                               .requires(mode, Mode.CREATIVE));
            CommandOption<Boolean, TestCommandContext> json = option(Options.flag("json", 'j'));
            CommandOption<Boolean, TestCommandContext> yaml = option(Options.flag("yaml", 'y'));
            mutuallyExclusive(json, yaml);
            argument(new StrArg("file")).execute((file, ctx) -> ctx.sendMessage(file + " " + ctx.getOption(force)
                    + " " + ctx.getOption(reason) + " " + ctx.getOption(mode) + " " + ctx.getOption(threads) + " "
                    + ctx.getOption(json) + " " + ctx.getOption(yaml)));
        }};
    }

    @Test
    void option_annotations_match_the_fluent_options() throws Exception {
        TestCommand annotated = parser().parse(new AnnotatedExport());
        TestCommand fluent = fluentExport();

        assertSameCommand(annotated, fluent);
        List<String> inputs = List.of("export",
                                      "export data",
                                      "export -f -r late data",
                                      "export -r late data",
                                      "export --threads 4 data",
                                      "export -m creative --threads 4 data",
                                      "export -jf data",
                                      "export -j -y data");
        assertThat(transcriptOrError(annotated, inputs)).isEqualTo(transcriptOrError(fluent, inputs))
                                                        .contains("export -f -r late data -> data true late SURVIVAL 1 false false",
                                                                  "export -m creative --threads 4 data -> data false  CREATIVE 4 false false",
                                                                  "export -jf data -> data true  SURVIVAL 1 true false")
                                                        .anyMatch(x -> x.startsWith("export -j -y data -> rejected"))
                                                        .anyMatch(x -> x.startsWith("export -> ")
                                                                && x.contains("/export -j data"));
        assertThat(CommandDocs.of("test.command", annotated)
                              .permissionsMarkdown()).contains("test.command.export.option.reason");
    }

    @Command("team")
    @Middleware("audit")
    static final class AnnotatedTeam {
        @Execute
        void list(TestCommandContext ctx) {
            ctx.sendMessage("teams");
        }

        @Subcommand("<team>")
        @Description(value = "Show a team", caption = "test.team.show")
        void show(TestCommandContext ctx, @Name("team") String team) {
            ctx.sendMessage("team " + team);
        }

        @Subcommand("<team> add")
        @Description("Add players")
        @VisibleWhen("open")
        void add(TestCommandContext ctx,
                 @Name("team") String team,
                 @Name("players") @Variadic(max = 3) List<String> players) {
            ctx.sendMessage("add " + players + " to " + team);
        }

        @Subcommand("<team> remove")
        @OptionsAnywhere
        void remove(TestCommandContext ctx,
                    @Flag(value = "silent", shortName = 's') boolean silent,
                    @Name("team") String team,
                    @Name("player") String player) {
            ctx.sendMessage("remove " + player + " from " + team + " " + silent);
        }
    }

    private static TestCommand fluentTeam(CommandMiddleware<TestCommandContext> audit, boolean[] open) {
        Caption show = Caption.of("test.team.show", "Show a team");
        return new TestCommand("team") {{
            addMiddleware(audit);
            execute(ctx -> ctx.sendMessage("teams"));
            argument(new StrArg("team")).description(ctx -> ctx.caption(show))
                                        .execute((team, ctx) -> ctx.sendMessage("team " + team))
                                        .child(team -> new TestCommand("add") {{
                                            description("Add players");
                                            visibleWhen(ctx -> open[0]);
                                            argument(new StrArg("players").variadic(3)).execute((players,
                                                                                                 ctx) -> ctx.sendMessage("add "
                                                                                                         + players
                                                                                                         + " to "
                                                                                                         + team.get()));
                                        }})
                                        .child(team -> new TestCommand("remove") {{
                                            optionsAnywhere();
                                            CommandOption<Boolean, TestCommandContext> silent = option(Options.flag("silent",
                                                                                                                    's'));
                                            argument(new StrArg("player")).execute((player,
                                                                                    ctx) -> ctx.sendMessage("remove "
                                                                                            + player + " from "
                                                                                            + team.get() + " "
                                                                                            + ctx.getOption(silent)));
                                        }});
        }};
    }

    @Test
    void placeholders_middlewares_and_visibility_match_the_fluent_command() throws Exception {
        boolean[] open = {true};
        CommandMiddleware<TestCommandContext> audit = (ctx, next) -> {
            ctx.sendMessage("audit");
            return next.proceed();
        };
        TestCommand annotated = parser().registerMiddleware("audit", audit)
                                        .registerVisibility("open", ctx -> open[0])
                                        .parse(new AnnotatedTeam());
        TestCommand fluent = fluentTeam(audit, open);

        assertSameCommand(annotated, fluent);
        List<String> inputs = List.of("team",
                                      "team red",
                                      "team red add Steve Alex",
                                      "team red add a b c d",
                                      "team red remove Steve -s");
        assertThat(transcriptOrError(annotated, inputs)).isEqualTo(transcriptOrError(fluent, inputs))
                                                        .contains("team -> audit | teams",
                                                                  "team red -> audit | team red",
                                                                  "team red add Steve Alex -> audit | add [Steve, Alex] to red",
                                                                  "team red remove Steve -s -> audit | remove Steve from red true")
                                                        .anyMatch(x -> x.startsWith("team red add a b c d -> rejected"));
        open[0] = false;
        List<String> closed = List.of("team red add Steve");
        assertThat(transcriptOrError(annotated, closed)).isEqualTo(transcriptOrError(fluent, closed))
                                                        .allMatch(x -> x.contains("rejected"));
    }

    @Command("pair")
    static final class AdjacentPlaceholders {
        @Subcommand("<a> <b> swap")
        void swap(TestCommandContext ctx, @Name("b") int b, @Name("a") String a) {
            ctx.sendMessage(b + " " + a);
        }
    }

    @Test
    void adjacent_placeholders_form_one_branch() throws Exception {
        TestCommand annotated = parser().parse(new AdjacentPlaceholders());
        TestCommand fluent = new TestCommand("pair") {{
            argument(new StrArg("a"), new IntArg("b")).child((a, b) -> new TestCommand("swap") {{
                execute(ctx -> ctx.sendMessage(b.get() + " " + a.get()));
            }});
        }};

        assertSameCommand(annotated, fluent);
        assertThat(new TestCommandRunner(annotated).execute("pair x 3 swap")
                                                   .messages()).containsExactly("3 x");
    }

    @Command("x")
    static final class UnknownMiddleware {
        @Subcommand("a")
        @Middleware("audit")
        void a() {
        }
    }

    @Command("x")
    static final class UnknownVisibility {
        @Execute
        @VisibleWhen("open")
        void run(@Name("n") int n) {
        }
    }

    @Command("x")
    static final class UnknownRequiredOption {
        @Execute
        void run(@Flag("force") @Requires("yes") boolean force) {
        }
    }

    @Command("x")
    @MutuallyExclusive({"json", "yaml"})
    static final class UnknownExclusiveOption {
        @Execute
        void run(@Flag("json") boolean json) {
        }
    }

    @Command("x")
    static final class MissingPlaceholder {
        @Subcommand("<team> add")
        void add(@Name("name") String name) {
        }
    }

    @Command("x")
    static final class DifferentPlaceholders {
        @Subcommand("<team> add")
        void add(@Name("team") String team) {
        }

        @Subcommand("<team> remove")
        void remove(@Name("team") int team) {
        }
    }

    @Command("x")
    static final class ArgumentsAfterPlaceholder {
        @Subcommand("<team>")
        void show(@Name("team") String team, @Name("page") int page) {
        }
    }

    @Command("x")
    static final class VariadicString {
        @Execute
        void run(@Name("text") @Variadic(max = 2) String text) {
        }
    }

    @Command("x")
    static final class ArgumentPermission {
        @Execute
        void run(@Name("text") @Permission(defaultPermission = DefaultPermission.OP) String text) {
        }
    }

    @Command("x")
    static final class CaptionedOption {
        @Execute
        void run(@Flag("force") @Description(value = "Force", caption = "x.force") boolean force) {
        }
    }

    @Command("x")
    static final class UnknownOptionType {
        @Execute
        void run(@Option("thread") Thread thread) {
        }
    }

    @Command("x")
    static final class ConfirmedPlaceholder {
        @Subcommand("<team>")
        @Confirm
        void show(@Name("team") String team) {
        }
    }

    @Test
    void invalid_uses_of_the_newer_annotations_fail_with_the_method() {
        AnnotationParser<TestCommandContext, TestCommand> parser = parser();

        assertThatThrownBy(() -> parser.parse(new UnknownMiddleware())).hasMessageContaining("@Middleware(\"audit\"), but no middleware of that name is registered; register one with registerMiddleware");
        assertThatThrownBy(() -> parser.parse(new UnknownVisibility())).hasMessageContaining("@VisibleWhen(\"open\"), but no visibility predicate");
        assertThatThrownBy(() -> parser.parse(new UnknownRequiredOption())).hasMessageContaining("has @Requires(\"yes\"), but 'x' has no option of that name");
        assertThatThrownBy(() -> parser.parse(new UnknownExclusiveOption())).hasMessageContaining("@MutuallyExclusive with --yaml");
        assertThatThrownBy(() -> parser.parse(new MissingPlaceholder())).hasMessageContaining("has <team> in its @Subcommand path, but no argument parameter named team");
        assertThatThrownBy(() -> parser.parse(new DifferentPlaceholders())).hasMessageContaining("declares the arguments of '<team>' differently");
        assertThatThrownBy(() -> parser.parse(new ArgumentsAfterPlaceholder())).hasMessageContaining("takes arguments after the placeholders of its path");
        assertThatThrownBy(() -> parser.parse(new VariadicString())).hasMessageContaining("has @Variadic, which applies to a List argument");
        assertThatThrownBy(() -> parser.parse(new ArgumentPermission())).hasMessageContaining("has @Permission, which does not apply to an argument");
        assertThatThrownBy(() -> parser.parse(new CaptionedOption())).hasMessageContaining("has @Description with a caption");
        assertThatThrownBy(() -> parser.parse(new UnknownOptionType())).hasMessageContaining("has @Option, but there is no argument type for java.lang.Thread");
        assertThatThrownBy(() -> parser.parse(new ConfirmedPlaceholder())).hasMessageContaining("has @Confirm; its path ends in a placeholder");
    }

    @Command("x")
    static final class ExecuteAndSubcommand {
        @Execute
        @Subcommand("a")
        void run() {
        }
    }

    @Command("x")
    static final class TwoExecutors {
        @Execute
        void one() {
        }

        @Execute
        void two() {
        }
    }

    @Command("x")
    static final class TwoPlaceholderExecutors {
        @Subcommand("<team>")
        void one(@Name("team") String team) {
        }

        @Subcommand("<team>")
        void two(@Name("team") String team) {
        }
    }

    @Command("x")
    static final class EmptySubcommand {
        @Subcommand(" ")
        void run() {
        }
    }

    @Command("x")
    static final class PlaceholderAliases {
        @Subcommand(value = "<team>", aliases = "t")
        void show(@Name("team") String team) {
        }
    }

    @Command("x")
    @Permission(defaultPermission = {DefaultPermission.OP, DefaultPermission.ALL})
    static final class TwoDefaultPermissions {
        @Execute
        void run() {
        }
    }

    @Command("x")
    static final class GreedyInteger {
        @Execute
        void run(@Name("n") @Greedy int n) {
        }
    }

    @Command("x")
    static final class VariadicDefault {
        @Execute
        void run(@Name("names") @Default("a") List<String> names) {
        }
    }

    @Command("x")
    static final class OptionalWithoutValue {
        @Execute
        void run(@Name("mode") @Optional Mode mode) {
        }
    }

    @Command("x")
    static final class EmptySuggest {
        @Execute
        void run(@Name("name") @Suggest String name) {
        }
    }

    @Command("x")
    static final class MissingSuggestMethod {
        @Execute
        void run(@Name("name") @Suggest(method = "names") String name) {
        }
    }

    @Command("x")
    static final class FlagAndOption {
        @Execute
        void run(@Flag("a") @Option("a") boolean a) {
        }
    }

    @Command("x")
    static final class BooleanFlagWithMax {
        @Execute
        void run(@Flag(value = "v", max = 3) boolean v) {
        }
    }

    @Command("x")
    static final class CountedFlagWithoutMax {
        @Execute
        void run(@Flag("v") int v) {
        }
    }

    @Command("x")
    static final class StringFlag {
        @Execute
        void run(@Flag("v") String v) {
        }
    }

    @Command("x")
    static final class TwoEqualToValues {
        @Execute
        void run(@Option("mode") String mode,
                 @Flag("force") @Requires(value = "mode", equalTo = {"a", "b"}) boolean force) {
        }
    }

    @Command("x")
    static final class EqualToRepeatable {
        @Execute
        void run(@Option(value = "tag", repeatable = 2) List<String> tag,
                 @Flag("force") @Requires(value = "tag", equalTo = "a") boolean force) {
        }
    }

    @Command("x")
    static final class ListOptionWithoutRepeatable {
        @Execute
        void run(@Option("tag") List<String> tag) {
        }
    }

    @Command("x")
    static final class RepeatableWithoutList {
        @Execute
        void run(@Option(value = "tag", repeatable = 2) String tag) {
        }
    }

    @Command("x")
    static final class RepeatableWithDefault {
        @Execute
        void run(@Option(value = "tag", repeatable = 2) @Default("a") List<String> tag) {
        }
    }

    @Command("x")
    static final class RangeOnLongOption {
        @Execute
        void run(@Option("n") @Range(max = 3) long n) {
        }
    }

    @Command("x")
    static final class OptionPermissionWithoutNode {
        @Execute
        void run(@Option("n") @Permission int n) {
        }
    }

    @Command("x")
    static final class RawList {
        @Execute
        @SuppressWarnings("rawtypes")
        void run(@Name("names") List names) {
        }
    }

    @Command("x")
    static final class BadEnumDefault {
        @Execute
        void run(@Name("mode") @Default("adventure") Mode mode) {
        }
    }

    @Test
    void conflicting_methods_and_misused_parameters_fail_with_the_method() {
        AnnotationParser<TestCommandContext, TestCommand> parser = parser();

        assertThatThrownBy(() -> parser.parse(new ExecuteAndSubcommand())).hasMessageContaining("ExecuteAndSubcommand#run() has both @Execute and @Subcommand");
        assertThatThrownBy(() -> parser.parse(new TwoExecutors())).hasMessageContaining("executes '' without arguments, as another method does");
        assertThatThrownBy(() -> parser.parse(new TwoPlaceholderExecutors())).hasMessageContaining("executes '<team>', as another method does");
        assertThatThrownBy(() -> parser.parse(new EmptySubcommand())).hasMessageContaining("has an empty @Subcommand; use @Execute for the command itself");
        assertThatThrownBy(() -> parser.parse(new PlaceholderAliases())).hasMessageContaining("has aliases, but its path ends in a placeholder");
        assertThatThrownBy(() -> parser.parse(new TwoDefaultPermissions())).hasMessageContaining("gives more than one defaultPermission");
        assertThatThrownBy(() -> parser.parse(new GreedyInteger())).hasMessageContaining("parameter n has @Greedy, but java.lang.Integer is not a String");
        assertThatThrownBy(() -> parser.parse(new VariadicDefault())).hasMessageContaining("parameter names is variadic and cannot take @Default");
        assertThatThrownBy(() -> parser.parse(new OptionalWithoutValue())).hasMessageContaining("has no value for an omitted "
                + Mode.class.getName());
        assertThatThrownBy(() -> parser.parse(new EmptySuggest())).hasMessageContaining("has @Suggest without values or a method");
        assertThatThrownBy(() -> parser.parse(new MissingSuggestMethod())).hasMessageContaining("names the suggestion method 'names', but MissingSuggestMethod has 0 methods");
        assertThatThrownBy(() -> parser.parse(new FlagAndOption())).hasMessageContaining("has both @Flag and @Option");
        assertThatThrownBy(() -> parser.parse(new BooleanFlagWithMax())).hasMessageContaining("is a boolean flag; max applies to an int flag");
        assertThatThrownBy(() -> parser.parse(new CountedFlagWithoutMax())).hasMessageContaining("is a counted flag and needs max of at least 1");
        assertThatThrownBy(() -> parser.parse(new StringFlag())).hasMessageContaining("has @Flag, which needs a boolean or int parameter");
        assertThatThrownBy(() -> parser.parse(new TwoEqualToValues())).hasMessageContaining("has @Requires with more than one equalTo value");
        assertThatThrownBy(() -> parser.parse(new EqualToRepeatable())).hasMessageContaining("has @Requires(\"tag\") with equalTo, but --tag is repeatable");
        assertThatThrownBy(() -> parser.parse(new ListOptionWithoutRepeatable())).hasMessageContaining("is a List option and needs repeatable of at least 1");
        assertThatThrownBy(() -> parser.parse(new RepeatableWithoutList())).hasMessageContaining("has repeatable, which needs a List parameter");
        assertThatThrownBy(() -> parser.parse(new RepeatableWithDefault())).hasMessageContaining("is repeatable and cannot take @Default or @Optional");
        assertThatThrownBy(() -> parser.parse(new RangeOnLongOption())).hasMessageContaining("has @Range, which applies to int options only");
        assertThatThrownBy(() -> parser.parse(new OptionPermissionWithoutNode())).hasMessageContaining("has @Permission, which on an option needs a node or one defaultPermission");
        assertThatThrownBy(() -> parser.parse(new RawList())).hasMessageContaining("parameter names needs a List of a class");
        assertThatThrownBy(() -> parser.parse(new BadEnumDefault())).hasMessageContaining("has @Default(\"adventure\"), which is not a valid "
                + Mode.class.getName() + "; it takes strings");
    }

    @Command("x")
    static final class ExecuteAndPlaceholderChild {
        @Execute
        void show(TestCommandContext ctx, @Name("team") String team) {
            ctx.sendMessage("show " + team);
        }

        @Subcommand("<team> add")
        void add(TestCommandContext ctx, @Name("team") String team) {
            ctx.sendMessage("add " + team);
        }
    }

    @Command("x")
    static final class ExecuteAndPlaceholder {
        @Execute
        void show(@Name("team") String team) {
        }

        @Subcommand("<team>")
        void other(@Name("team") String team) {
        }
    }

    @Test
    void execute_keeps_running_beside_a_subcommand_below_its_argument() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(parser().parse(new ExecuteAndPlaceholderChild()));

        assertThat(runner.execute("x red")
                         .messages()).containsExactly("show red");
        assertThat(runner.execute("x red add")
                         .messages()).containsExactly("add red");
    }

    @Test
    void execute_and_subcommand_running_at_the_same_argument_are_rejected() {
        TestCommand command = parser().parse(new ExecuteAndPlaceholder());

        assertThatThrownBy(() -> new TestCommandRunner(command)).isInstanceOf(IllegalStateException.class)
                                                                .hasMessageContaining("more than one argument branch that runs for input ending at <team>");
    }

    private static void assertSameCommand(TestCommand annotated, TestCommand fluent) {
        assertThat(CommandTreeRenderer.render(List.of(annotated),
                                              "test.command")).isEqualTo(CommandTreeRenderer.render(List.of(fluent),
                                                                                                    "test.command"));
        assertThat(CommandDocs.of("test.command", annotated)
                              .json()).isEqualTo(CommandDocs.of("test.command", fluent)
                                                            .json());
        assertThat(CommandDocs.of("test.command", annotated)
                              .markdown()).isEqualTo(CommandDocs.of("test.command", fluent)
                                                                .markdown());
    }

    /**
     * What each input sends, or how Brigadier rejects it.
     */
    private static List<String> transcriptOrError(TestCommand command, List<String> inputs) {
        TestCommandRunner runner = new TestCommandRunner(command);
        List<String> lines = new ArrayList<>();
        for (String input : inputs) {
            try {
                lines.add(input + " -> " + String.join(" | ",
                                                       runner.execute(input)
                                                             .messages()));
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                lines.add(input + " -> rejected: " + e.getMessage());
            }
        }
        return lines;
    }
}
