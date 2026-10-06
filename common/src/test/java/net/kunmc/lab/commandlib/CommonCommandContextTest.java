package net.kunmc.lab.commandlib;

import net.kunmc.lab.commandlib.argument.CommonIntegerArgument;
import net.kunmc.lab.commandlib.argument.CommonStringArgument;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class CommonCommandContextTest {
    @Test
    void arguments_can_be_read_by_index_name_and_argument_instance() throws Exception {
        CommonIntegerArgument<TestCommandContext, ?> left = new CommonIntegerArgument<>("left");
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("sum") {{
            argument(left, new CommonStringArgument<>("label")).execute((l, label, ctx) -> {
                ctx.sendMessage(ctx.getArgument(0, Integer.class));
                ctx.sendMessage(ctx.getArgument(1));
                ctx.sendMessage(ctx.getArgument("label", String.class));
                ctx.sendMessage(ctx.getArgument(left));
                ctx.sendMessage(ctx.getInput(1));
            });
        }});

        assertThat(runner.execute("sum 4 four")
                         .messages()).containsExactly("4", "four", "four", "4", "4");
    }

    @Test
    void missing_argument_lookups_fail_clearly() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("sum") {{
            argument(new CommonIntegerArgument<>("value")).execute((value, ctx) -> {
                ctx.sendMessage(ctx.getInput("missing").isEmpty());
                try {
                    ctx.getArgument("missing");
                } catch (IllegalArgumentException e) {
                    ctx.sendMessage(e.getMessage());
                }
                try {
                    ctx.getArgument(5);
                } catch (IndexOutOfBoundsException e) {
                    ctx.sendMessage("index");
                }
                try {
                    ctx.getArgument("value", String.class);
                } catch (ClassCastException e) {
                    ctx.sendMessage("cast");
                }
            });
        }});

        assertThat(runner.execute("sum 1")
                         .messages()).containsExactly("true",
                                                      "No such argument 'missing' exists on this command",
                                                      "index",
                                                      "cast");
    }

    @Test
    void object_send_methods_stringify_null() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("say") {{
            execute(ctx -> {
                ctx.sendMessage((Object) null);
                ctx.sendSuccess((Object) null);
                ctx.sendWarn((Object) null);
                ctx.sendFailure((Object) null);
                ctx.sendMessage((Object) 42);
            });
        }});

        assertThat(runner.execute("say")
                         .messages()).containsExactly("null", "null", "null", "null", "42");
    }

    @Test
    void default_language_follows_jvm_locale() throws Exception {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.JAPAN);
            TestCommandRunner runner = new TestCommandRunner(new TestCommand("lang") {{
                execute(ctx -> {
                    ctx.sendMessage(ctx.getLanguage());
                    ctx.sendMessage(ctx.getLocale());
                });
            }});

            assertThat(runner.execute("lang")
                             .messages()).containsExactly("ja_jp", "ja_JP");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void current_context_is_cleared_after_execution() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("run") {{
            execute(ctx -> ctx.sendMessage(CurrentCommandContext.get() == ctx));
        }});

        assertThat(runner.execute("run")
                         .messages()).containsExactly("true");
        assertThat(CurrentCommandContext.get()).isNull();
    }

    @Test
    void multiple_prerequisites_are_checked_in_order() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("run") {{
            addPrerequisite(ctx -> ctx.sendMessage("first"));
            addPrerequisite(ctx -> {
                throw new net.kunmc.lab.commandlib.exception.CommandPrerequisiteException("second failed");
            });
            addPrerequisite(ctx -> ctx.sendMessage("third"));
            execute(ctx -> ctx.sendMessage("executed"));
        }});

        assertThat(runner.execute("run")
                         .messages()).containsExactly("first", "second failed");
    }

    @Test
    void consumer_preprocess_runs_before_executor_and_does_not_cancel() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("run") {{
            addPreprocess(ctx -> {
                ctx.sendMessage("pre");
            });
            execute(ctx -> ctx.sendMessage("executed"));
        }});

        assertThat(runner.execute("run")
                         .messages()).containsExactly("pre", "executed");
    }

    @Test
    void preprocess_is_not_run_when_command_falls_back_to_help() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            addPreprocess(ctx -> {
                ctx.sendMessage("pre");
            });
            addChildren(new TestCommand("start") {{
                execute(ctx -> ctx.sendMessage("start"));
            }});
        }});

        assertThat(runner.execute("game")
                         .messages()).doesNotContain("pre")
                                     .anyMatch(x -> x.contains("Usage:"));
    }

    @Test
    void grandchild_inherits_prerequisites_of_all_ancestors() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("a") {{
            addPrerequisite(ctx -> ctx.sendMessage("a"));
            addChildren(new TestCommand("b") {{
                addPrerequisite(ctx -> ctx.sendMessage("b"));
                addChildren(new TestCommand("c") {{
                    execute(ctx -> ctx.sendMessage("c"));
                }});
            }});
        }});

        assertThat(runner.execute("a b c")
                         .messages()).containsExactly("a", "b", "c");
    }

    @Test
    void executor_can_throw_prerequisite_exception_to_send_failure() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("run") {{
            execute(ctx -> {
                throw new net.kunmc.lab.commandlib.exception.CommandPrerequisiteException("not now");
            });
        }});

        assertThat(runner.executeAndGetResult("run")).isZero();
        assertThat(runner.execute("run")
                         .messages()).containsExactly("not now");
    }
}
