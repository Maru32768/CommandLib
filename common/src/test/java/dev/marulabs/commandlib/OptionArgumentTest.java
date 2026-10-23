package dev.marulabs.commandlib;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.argument.CommonEnumArgument;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Options whose value a CommandLib argument parses ({@link Options#of}).
 */
class OptionArgumentTest {
    enum Mode {
        FAST,
        SAFE
    }

    private static TestCommand scan() {
        return new TestCommand("scan") {{
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Integer, TestCommandContext> verbose = option(Options.count("verbose", 'v', 2));
            CommandOption<Mode, TestCommandContext> mode = option(Options.of("mode",
                                                                             'm',
                                                                             new ModeArgument("mode").optional(Mode.SAFE)));
            CommandOption<Point, TestCommandContext> at = option(Options.of("at",
                                                                            new PointArgument("at").validator(x -> x.x >= 0)
                                                                                                   .optional(new Point(0,
                                                                                                                       0))));
            argument(new CommonStringArgument<>("target")).execute((ctx, target) -> {
                ctx.sendMessage(target + " force=" + ctx.getOption(force) + " verbose=" + ctx.getOption(verbose)
                        + " mode=" + ctx.getOption(mode) + " at=" + ctx.getOption(at) + " given="
                        + ctx.hasOption(mode) + " inputs=" + ctx.getInputs());
            });
        }};
    }

    @Test
    void the_value_is_parsed_by_the_argument_before_and_after_redirects() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(scan());

        assertThat(runner.execute("scan -f --mode fast -v --at 3,4 Alex")
                         .messages()).containsExactly("Alex force=true verbose=1 mode=FAST at=3,4 given=true inputs=[scan, Alex]");
        assertThat(runner.execute("scan -v -m FAST -f Alex")
                         .messages()).containsExactly("Alex force=true verbose=1 mode=FAST at=0,0 given=true inputs=[scan, Alex]");
        assertThat(runner.execute("scan --at 1,2 -m safe -vv Alex")
                         .messages()).containsExactly("Alex force=false verbose=2 mode=SAFE at=1,2 given=true inputs=[scan, Alex]");
    }

    @Test
    void a_command_with_a_type_that_is_no_brigadier_primitive_redirects_to_the_node_of_the_same_options() {
        TestCommandRunner runner = new TestCommandRunner(scan());

        assertThat(OptionTrees.assertRedirectsReachTheSameOptions(runner.root()
                                                                        .getChild("scan"),
                                                                  Map.of("force",
                                                                         "force",
                                                                         "f",
                                                                         "force",
                                                                         "verbose",
                                                                         "verbose",
                                                                         "v",
                                                                         "verbose",
                                                                         "mode",
                                                                         "mode",
                                                                         "m",
                                                                         "mode",
                                                                         "at",
                                                                         "at"))).isPositive();
    }

    @Test
    void the_default_of_an_optional_argument_applies_when_the_option_is_not_given() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(scan());

        assertThat(runner.execute("scan Alex")
                         .messages()).containsExactly("Alex force=false verbose=0 mode=SAFE at=0,0 given=false inputs=[scan, Alex]");
    }

    @Test
    void the_argument_rejects_a_value_it_does_not_parse_or_validate() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(scan());

        // The argument's own error, with the rejected value in the context line.
        assertThat(runner.execute("scan -m slow Alex")
                         .messages()).first()
                                     .isEqualTo("command.unknown.argument");
        assertThat(runner.execute("scan -m slow Alex")
                         .messages()).anyMatch(x -> x.contains("slow"));
        assertThat(runner.execute("scan -f --at -1,2 Alex")
                         .messages()).first()
                                     .isEqualTo("command.unknown.argument");
        assertThat(runner.execute("scan -f --at -1,2 Alex")
                         .messages()).anyMatch(x -> x.contains("-1,2"));
        // The type itself rejects the value while Brigadier parses.
        assertThatThrownBy(() -> runner.execute("scan --at 1 Alex")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void the_option_of_an_argument_that_is_not_optional_is_required() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("deploy") {{
            CommandOption<Mode, TestCommandContext> mode = option(Options.of("mode",
                                                                             'm',
                                                                             new ModeArgument("mode")));
            execute(ctx -> ctx.sendMessage("mode=" + ctx.getOption(mode)));
        }});

        assertThat(runner.execute("deploy -m fast")
                         .messages()).containsExactly("mode=FAST");
        assertThat(runner.execute("deploy")
                         .messages()).containsExactly("Missing required option --mode.");
    }

    @Test
    void the_value_is_suggested_by_the_argument() {
        TestCommandRunner runner = new TestCommandRunner(scan());

        assertThat(runner.suggest("scan --mode ")).containsExactly("fast", "safe");
        assertThat(runner.suggest("scan -f -m fa")).containsExactly("fast");
    }

    @Test
    void an_option_and_a_positional_argument_can_share_a_name() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("give") {{
            CommandOption<String, TestCommandContext> target = option(Options.of("target",
                                                                                 't',
                                                                                 new CommonStringArgument<>("target").optional("nobody")));
            argument(new CommonStringArgument<>("target")).execute((ctx, positional) -> ctx.sendMessage(positional
                    + " " + ctx.getOption(target)));
        }});

        assertThat(runner.execute("give -t Bob Alex")
                         .messages()).containsExactly("Alex Bob");
        assertThat(runner.execute("give Alex")
                         .messages()).containsExactly("Alex nobody");
    }

    @Test
    void repeatable_options_of_an_argument_collect_the_parsed_values() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("modes") {{
            CommandOption<List<Mode>, TestCommandContext> modes = option(Options.of("mode",
                                                                                    'm',
                                                                                    new ModeArgument("mode"))
                                                                                .repeatable(2));
            execute(ctx -> ctx.sendMessage("modes=" + ctx.getOption(modes)));
        }});

        assertThat(runner.execute("modes -m fast --mode safe")
                         .messages()).containsExactly("modes=[FAST, SAFE]");
        assertThat(runner.execute("modes")
                         .messages()).containsExactly("modes=[]");
        assertThat(runner.suggest("modes -m fast -m ")).containsExactly("fast", "safe");
    }

    @Test
    void a_variadic_argument_is_rejected() {
        assertThatThrownBy(() -> Options.of("modes",
                                            new ModeArgument("modes").variadic())).isInstanceOf(IllegalArgumentException.class);
    }

    private static final class ModeArgument extends CommonEnumArgument<Mode, TestCommandContext, ModeArgument> {
        ModeArgument(String name) {
            super(name, Mode.class);
        }
    }

    static final class Point {
        final int x;
        final int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return x + "," + y;
        }
    }

    // A type that is none of Brigadier's: two integers separated by a comma.
    private static final class PointType implements ArgumentType<Point> {
        @Override
        public Point parse(StringReader reader) throws CommandSyntaxException {
            int x = reader.readInt();
            reader.expect(',');
            return new Point(x, reader.readInt());
        }
    }

    private static final class PointArgument extends CommonArgument<Point, TestCommandContext, PointArgument> {
        PointArgument(String name) {
            super(name, new PointType());
        }

        @Override
        public Point cast(Object parsedArgument) {
            return (Point) parsedArgument;
        }

        @Override
        protected Point parseImpl(TestCommandContext ctx) {
            return ctx.getHandle()
                      .getArgument(name(), Point.class);
        }
    }
}
