package dev.marulabs.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.argument.CommonEnumArgument;
import dev.marulabs.commandlib.argument.CommonIntegerArgument;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionalVariadicArgumentTest {
    @Test
    void omitted_optional_argument_gives_its_default() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("give") {{
            argument(new WordArg("item"), new IntArg("count").optional(1)).execute((item, count, ctx) -> {
                ctx.sendMessage(item + "x" + count);
            });
        }});

        assertThat(runner.execute("give apple")
                         .messages()).containsExactly("applex1");
        assertThat(runner.execute("give apple 5")
                         .messages()).containsExactly("applex5");
    }

    @Test
    void several_trailing_optional_arguments_can_be_omitted() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("tp") {{
            argument(new IntArg("x"),
                     new IntArg("y").optional(0),
                     new IntArg("z").optional(0)).execute((x, y, z, ctx) -> {
                         ctx.sendMessage(x + "," + y + "," + z);
                     });
        }});

        assertThat(runner.execute("tp 1")
                         .messages()).containsExactly("1,0,0");
        assertThat(runner.execute("tp 1 2")
                         .messages()).containsExactly("1,2,0");
        assertThat(runner.execute("tp 1 2 3")
                         .messages()).containsExactly("1,2,3");
    }

    @Test
    void branch_of_only_optional_arguments_runs_on_the_bare_command() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("count") {{
            argument(new IntArg("n").optional(ctx -> 10)).execute((n, ctx) -> ctx.sendMessage("n=" + n));
        }});

        assertThat(runner.execute("count")
                         .messages()).containsExactly("n=10");
        assertThat(runner.execute("count 3")
                         .messages()).containsExactly("n=3");
    }

    @Test
    void default_extractor_failure_is_sent_and_stops_the_command() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("heal") {{
            argument(new WordArg("target").optional(ctx -> {
                throw new CommandPrerequisiteException("Give a target.");
            })).execute((target, ctx) -> ctx.sendMessage("healed " + target));
        }});

        assertThat(runner.execute("heal")
                         .messages()).containsExactly("Give a target.");
        assertThat(runner.execute("heal Steve")
                         .messages()).containsExactly("healed Steve");
    }

    @Test
    void variadic_argument_gives_every_value() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("heal") {{
            argument(new IntArg("amount"), new WordArg("targets").variadic()).execute((amount, targets, ctx) -> {
                List<String> names = targets;
                ctx.sendMessage(amount + ":" + names + ":" + ctx.getInput("targets"));
            });
        }});

        assertThat(runner.execute("heal 2 Steve")
                         .messages()).containsExactly("2:[Steve]:Steve");
        assertThat(runner.execute("heal 2 Steve Alex Maru")
                         .messages()).containsExactly("2:[Steve, Alex, Maru]:Steve Alex Maru");
    }

    @Test
    void variadic_argument_needs_one_value_unless_optional() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("heal") {{
            argument(new WordArg("targets").variadic()).execute((targets, ctx) -> ctx.sendMessage("unreachable"));
        }});
        TestCommandRunner optional = new TestCommandRunner(new TestCommand("heal") {{
            argument(new WordArg("targets").variadic()
                                           .optional(List.of())).execute((targets, ctx) -> ctx.sendMessage("healed " + targets));
        }});

        // Input without a value ends at the command, which shows its usage.
        assertThat(runner.execute("heal")
                         .messages()).anyMatch(x -> x.contains("/heal") && x.contains("targets..."));
        assertThat(optional.execute("heal")
                           .messages()).containsExactly("healed []");
        assertThat(optional.execute("heal Steve")
                           .messages()).containsExactly("healed [Steve]");
    }

    @Test
    void variadic_argument_accepts_up_to_its_max_count() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("sum") {{
            argument(new IntArg("n").variadic(2)).execute((n, ctx) -> ctx.sendMessage(n.stream()
                                                                                       .mapToInt(Integer::intValue)
                                                                                       .sum()));
        }});

        assertThat(runner.execute("sum 1 2")
                         .messages()).containsExactly("3");
        assertThatThrownBy(() -> runner.execute("sum 1 2 3")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void variadic_element_validator_points_at_its_own_token() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("go") {{
            argument(new DirArg("dirs").validator(x -> x != Direction.NORTH)
                                       .variadic()).execute((dirs, ctx) -> ctx.sendMessage("unreachable"));
        }});

        TestCommandContext ctx = runner.execute("go south north");

        assertThat(ctx.messages()).containsExactly("command.unknown.argument",
                                                   "§7go south §c§nnorth§rcommand.context.here");
    }

    @Test
    void variadic_list_validator_and_transformer_see_every_value() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("go") {{
            argument(new DirArg("dirs").transformer(x -> x == Direction.SOUTH ? Direction.NORTH : Direction.WEST)
                                       .variadic()
                                       .validator(x -> x.size() > 1)).execute((dirs, ctx) -> ctx.sendMessage(dirs));
        }});

        assertThat(runner.execute("go south east")
                         .messages()).containsExactly("[NORTH, WEST]");
        assertThat(runner.execute("go south")
                         .messages()).first()
                                     .isEqualTo("command.unknown.argument");
    }

    @Test
    void variadic_argument_suggests_each_value() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("go") {{
            argument(new DirArg("dirs").variadic()).execute((dirs, ctx) -> {
            });
        }});

        assertThat(runner.suggest("go n")).containsExactly("north");
        assertThat(runner.suggest("go south n")).containsExactly("north");
    }

    @Test
    void branch_permission_applies_to_every_variadic_value() throws Exception {
        TestCommand command = new TestCommand("heal") {{
            argument(new WordArg("targets").variadic()).execute((targets, ctx) -> ctx.sendMessage("healed"));
        }};
        TestCommandRunner denied = new TestCommandRunner(command, new TestCommandSource("test.command.heal"));
        TestCommandRunner allowed = new TestCommandRunner(command,
                                                          new TestCommandSource("test.command.heal",
                                                                                "test.command.heal.targets"));

        assertThat(denied.visibleChildren("heal")).doesNotContain("targets");
        assertThatThrownBy(() -> denied.execute("heal Steve Alex")).isInstanceOf(CommandSyntaxException.class);
        assertThat(allowed.execute("heal Steve Alex")
                          .messages()).containsExactly("healed");
    }

    @Test
    void help_marks_optional_and_variadic_arguments() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("give") {{
            argument(new WordArg("item"), new IntArg("count").optional(1)).execute((item, count, ctx) -> {
            });
            argument(new WordArg("kit"), new WordArg("targets").variadic()).execute((kit, targets, ctx) -> {
            });
        }});

        List<String> help = runner.execute("give help")
                                  .messages()
                                  .stream()
                                  .map(x -> x.replaceAll("§.", ""))
                                  .collect(java.util.stream.Collectors.toList());

        assertThat(help).contains("  /give <item> [count]", "  /give <kit> <targets...>");
    }

    @Test
    void required_argument_after_optional_one_fails_registration() {
        TestCommand command = new TestCommand("give") {{
            argument(new IntArg("count").optional(1), new WordArg("item")).execute((count, item, ctx) -> {
            });
        }};

        assertThatThrownBy(() -> new TestCommandRunner(command)).isInstanceOf(IllegalStateException.class)
                                                                .hasMessageContaining("'item'")
                                                                .hasMessageContaining("optional argument 'count'");
    }

    @Test
    void variadic_argument_that_is_not_last_fails_registration() {
        TestCommand command = new TestCommand("give") {{
            argument(new WordArg("targets").variadic(),
                     new IntArg("count").optional(1)).execute((targets, count, ctx) -> {
                     });
        }};

        assertThatThrownBy(() -> new TestCommandRunner(command)).isInstanceOf(IllegalStateException.class)
                                                                .hasMessageContaining("Variadic argument 'targets'");
    }

    @Test
    void optional_branch_with_child_commands_fails_registration() {
        TestCommand command = new TestCommand("config") {{
            argument(new WordArg("key").optional("all")).child(key -> new TestCommand("get"));
        }};

        assertThatThrownBy(() -> new TestCommandRunner(command)).isInstanceOf(IllegalStateException.class)
                                                                .hasMessageContaining("cannot have child commands");
    }

    @Test
    void command_executor_with_branch_of_only_optional_arguments_fails_registration() {
        TestCommand command = new TestCommand("count") {{
            execute(ctx -> {
            });
            argument(new IntArg("n").optional(1)).execute((n, ctx) -> {
            });
        }};

        assertThatThrownBy(() -> new TestCommandRunner(command)).isInstanceOf(IllegalStateException.class)
                                                                .hasMessageContaining("more than one action");
    }

    @Test
    void variadic_of_an_optional_or_executing_element_is_rejected() {
        assertThatThrownBy(() -> new IntArg("n").optional(1)
                                                .variadic()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new IntArg("n").execute(ctx -> {
        })
                                                .variadic()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new IntArg("n").addUncaughtExceptionHandler((e, ctx) -> {
        })
                                                .variadic()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void variadic_of_variadic_and_zero_max_count_are_rejected() {
        assertThatThrownBy(() -> new WordArg("a").variadic()
                                                 .variadic()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new WordArg("a").variadic(0)).isInstanceOf(IllegalArgumentException.class);
    }

    private enum Direction {
        NORTH,
        SOUTH,
        EAST,
        WEST
    }

    private static final class IntArg extends CommonIntegerArgument<TestCommandContext, IntArg> {
        IntArg(String name) {
            super(name);
        }
    }

    private static final class WordArg extends CommonStringArgument<TestCommandContext, WordArg> {
        WordArg(String name) {
            super(name, Type.WORD);
        }
    }

    private static final class DirArg extends CommonEnumArgument<Direction, TestCommandContext, DirArg> {
        DirArg(String name) {
            super(name, Direction.class);
        }
    }
}
