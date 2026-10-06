package net.kunmc.lab.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.argument.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandStructureTest {
    @Test
    void shared_argument_instance_keeps_executor_per_branch() throws Exception {
        StrArg key = new StrArg("key");
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("set") {{
            argument(new CommonIntegerArgument<>("number"), key).execute((number, value, ctx) -> {
                ctx.sendMessage("number:" + value);
            });
            argument(new CommonBooleanArgument<>("flag"), key).execute((flag, value, ctx) -> {
                ctx.sendMessage("flag:" + value);
            });
        }});

        assertThat(runner.execute("set 5 x")
                         .messages()).containsExactly("number:x");
        assertThat(runner.execute("set true x")
                         .messages()).containsExactly("flag:x");
    }

    @Test
    void argument_builder_branch_executes_with_builder_executor() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("sum") {{
            argument(builder -> {
                builder.argument(new CommonIntegerArgument<>("left"))
                       .argument(new CommonIntegerArgument<>("right"));
                builder.execute(ctx -> ctx.sendMessage((int) ctx.getArgument("left") + (int) ctx.getArgument("right")));
            });
        }});

        assertThat(runner.execute("sum 20 22")
                         .messages()).containsExactly("42");
    }

    @Test
    void argument_builder_branch_execute_overrides_builder_executor() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("sum") {{
            argument(builder -> {
                builder.argument(new CommonIntegerArgument<>("value"));
                builder.execute(ctx -> ctx.sendMessage("builder"));
            }).execute(ctx -> ctx.sendMessage("branch"));
        }});

        assertThat(runner.execute("sum 1")
                         .messages()).containsExactly("branch");
    }

    @Test
    void argument_builder_keeps_executor_set_directly_on_last_argument() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("sum") {{
            argument(builder -> {
                builder.argument(new IntArg("value").execute(ctx -> ctx.sendMessage("argument")));
                builder.execute(ctx -> ctx.sendMessage("builder"));
            });
        }});

        assertThat(runner.execute("sum 1")
                         .messages()).containsExactly("argument");
    }

    @Test
    void empty_argument_builder_is_rejected() {
        assertThatThrownBy(() -> new TestCommand("empty") {{
            argument(builder -> {
            });
        }}).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void argument_builder_rejects_duplicate_argument_names() {
        assertThatThrownBy(() -> new TestCommand("dup") {{
            argument(builder -> builder.argument(new CommonIntegerArgument<>("value"))
                                       .argument(new CommonStringArgument<>("value")));
        }}).isInstanceOf(IllegalArgumentException.class)
           .hasMessageContaining("value");
    }

    @Test
    void argument_branch_rejects_duplicate_argument_names() {
        assertThatThrownBy(() -> new TestCommand("dup") {{
            argument(new CommonIntegerArgument<>("value"), new CommonStringArgument<>("value"));
        }}).isInstanceOf(IllegalArgumentException.class)
           .hasMessageContaining("value");
    }

    @Test
    void empty_command_name_is_rejected() {
        assertThatThrownBy(() -> new TestCommand("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duplicate_child_command_names_are_rejected_at_build() {
        TestCommand command = new TestCommand("game") {{
            addChildren(new TestCommand("start"), new TestCommand("start"));
        }};

        assertThatThrownBy(() -> new TestCommandRunner(command)).isInstanceOf(IllegalStateException.class)
                                                                .hasMessageContaining("start");
    }

    @Test
    void child_alias_conflicting_with_sibling_name_is_rejected_at_build() {
        TestCommand command = new TestCommand("game") {{
            addChildren(new TestCommand("start"), new TestCommand("begin") {{
                addAliases("start");
            }});
        }};

        assertThatThrownBy(() -> new TestCommandRunner(command)).isInstanceOf(IllegalStateException.class)
                                                                .hasMessageContaining("start");
    }

    @Test
    void user_defined_help_child_replaces_generated_help() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            addChildren(new TestCommand("start") {{
                execute(ctx -> ctx.sendMessage("start"));
            }}, new TestCommand("help") {{
                execute(ctx -> ctx.sendMessage("custom help"));
            }});
        }});

        assertThat(runner.execute("game help")
                         .messages()).containsExactly("custom help");
    }

    @Test
    void generated_help_literal_prints_usage() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            addChildren(new TestCommand("start") {{
                description("Start the game");
                execute(ctx -> ctx.sendMessage("start"));
            }});
        }});

        TestCommandContext ctx = runner.execute("game help");

        assertThat(ctx.messages()).anyMatch(x -> x.contains("Usage:"));
        assertThat(ctx.messages()).anyMatch(x -> x.contains("start") && x.contains("Start the game"));
    }

    @Test
    void alias_shows_help_of_original_command() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            addAliases("g");
            addChildren(new TestCommand("start") {{
                description("Start the game");
                execute(ctx -> ctx.sendMessage("start"));
            }});
        }});

        TestCommandContext ctx = runner.execute("g");

        assertThat(ctx.messages()).anyMatch(x -> x.contains("Start the game"));
    }

    @Test
    void alias_executes_argument_branch() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("give") {{
            addAliases("g");
            argument(new CommonIntegerArgument<>("amount")).execute((amount, ctx) -> ctx.sendMessage("amount=" + amount));
        }});

        assertThat(runner.execute("g 3")
                         .messages()).containsExactly("amount=3");
    }

    @Test
    void three_level_nested_children_execute() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("a") {{
            addChildren(new TestCommand("b") {{
                addChildren(new TestCommand("c") {{
                    addChildren(new TestCommand("d") {{
                        execute(ctx -> ctx.sendMessage("d"));
                    }});
                }});
            }});
        }});

        assertThat(runner.execute("a b c d")
                         .messages()).containsExactly("d");
    }

    @Test
    void shorter_variable_length_branch_uses_its_own_executor() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("set") {{
            argument(new CommonStringArgument<>("name")).execute((name, ctx) -> ctx.sendMessage("one:" + name));
            argument(new CommonStringArgument<>("name"),
                     new CommonIntegerArgument<>("value")).execute((name, value, ctx) -> ctx.sendMessage("two:" + name + "=" + value));
        }});

        assertThat(runner.execute("set foo")
                         .messages()).containsExactly("one:foo");
        assertThat(runner.execute("set foo 3")
                         .messages()).containsExactly("two:foo=3");
    }

    @Test
    void every_branch_arity_passes_arguments_in_declaration_order() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("n") {{
            addChildren(new TestCommand("four") {{
                argument(i("a"), i("b"), i("c"), i("d")).execute((a, b, c, d, ctx) -> ctx.sendMessage(List.of(a,
                                                                                                                b,
                                                                                                                c,
                                                                                                                d)));
            }}, new TestCommand("five") {{
                argument(i("a"), i("b"), i("c"), i("d"), i("e")).execute((a, b, c, d, e, ctx) -> ctx.sendMessage(
                        List.of(a, b, c, d, e)));
            }}, new TestCommand("six") {{
                argument(i("a"), i("b"), i("c"), i("d"), i("e"), i("f")).execute((a, b, c, d, e, f, ctx) -> ctx.sendMessage(
                        List.of(a, b, c, d, e, f)));
            }}, new TestCommand("seven") {{
                argument(i("a"),
                         i("b"),
                         i("c"),
                         i("d"),
                         i("e"),
                         i("f"),
                         i("g")).execute((a, b, c, d, e, f, g, ctx) -> ctx.sendMessage(List.of(a, b, c, d, e, f, g)));
            }});
        }});

        assertThat(runner.execute("n four 1 2 3 4")
                         .messages()).containsExactly("[1, 2, 3, 4]");
        assertThat(runner.execute("n five 1 2 3 4 5")
                         .messages()).containsExactly("[1, 2, 3, 4, 5]");
        assertThat(runner.execute("n six 1 2 3 4 5 6")
                         .messages()).containsExactly("[1, 2, 3, 4, 5, 6]");
        assertThat(runner.execute("n seven 1 2 3 4 5 6 7")
                         .messages()).containsExactly("[1, 2, 3, 4, 5, 6, 7]");
    }

    @Test
    void command_uncaught_exception_handler_receives_executor_exception() throws Exception {
        List<String> handled = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("boom") {{
            addUncaughtExceptionHandler((e, ctx) -> handled.add("command:" + e.getMessage()));
            execute(ctx -> {
                throw new IllegalStateException("bare");
            });
        }});

        TestCommandContext ctx = runner.execute("boom");

        assertThat(handled).containsExactly("command:bare");
        assertThat(ctx.messages()).containsExactly("An unexpected error occurred trying to execute that command.",
                                                   "Check the console for details.");
    }

    @Test
    void command_and_argument_uncaught_exception_handlers_receive_argument_executor_exception() throws Exception {
        List<String> handled = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("boom") {{
            addUncaughtExceptionHandler((e, ctx) -> handled.add("command:" + e.getMessage()));
            argument(new IntArg("value").addUncaughtExceptionHandler((e, ctx) -> handled.add("argument:" + e.getMessage()))).execute(
                    (value, ctx) -> {
                        throw new IllegalStateException("value=" + value);
                    });
        }});

        runner.execute("boom 3");

        assertThat(handled).containsExactly("command:value=3", "argument:value=3");
    }

    @Test
    void uncaught_exception_handler_receives_transformer_exception() {
        List<String> handled = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("boom") {{
            addUncaughtExceptionHandler((e, ctx) -> handled.add(e.getMessage()));
            argument(new IntArg("value").transformer(x -> {
                throw new IllegalStateException("transform");
            })).execute((value, ctx) -> ctx.sendMessage("unreachable"));
        }});

        assertThatThrownBy(() -> runner.execute("boom 1")).isInstanceOf(IllegalStateException.class)
                                                          .hasMessage("transform");
        assertThat(handled).containsExactly("transform");
    }

    @Test
    void parse_failure_returns_zero_result() throws CommandSyntaxException {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("go") {{
            argument(new CommonLiteralArgument<>("dir", List.of("north"))).execute((dir, ctx) -> {
            });
        }});

        assertThat(runner.executeAndGetResult("go north")).isEqualTo(1);
        assertThat(runner.executeAndGetResult("go south")).isZero();
    }

    @Test
    void option_parse_failure_returns_zero_result() throws CommandSyntaxException {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("go") {{
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Boolean, TestCommandContext> verbose = option(Options.flag("verbose", 'v')
                                                                               .requires(force));
            execute(ctx -> {
            });
        }});

        assertThat(runner.executeAndGetResult("go -v")).isZero();
    }

    private static IntArg i(String name) {
        return new IntArg(name);
    }

    private static final class IntArg extends CommonIntegerArgument<TestCommandContext, IntArg> {
        IntArg(String name) {
            super(name);
        }
    }

    private static final class StrArg extends CommonStringArgument<TestCommandContext, StrArg> {
        StrArg(String name) {
            super(name);
        }
    }
}
