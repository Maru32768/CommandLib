package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.CommonIntegerArgument;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommonCommandRequireTest {
    private static final Extractor<TestCommandContext, String> SUCCEEDING_EXTRACTOR = ctx -> "extracted";
    private static final Extractor<TestCommandContext, String> FAILING_EXTRACTOR = ctx -> {
        throw new CommandPrerequisiteException("prerequisite failed");
    };

    @Test
    void require_on_command_runs_execute_with_extracted_value() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("cmd") {{
            require(SUCCEEDING_EXTRACTOR).execute((ctx, s) -> ctx.sendMessage("got:" + s));
        }});

        TestCommandContext ctx = runner.execute("cmd");

        assertThat(ctx.messages()).containsExactly("got:extracted");
    }

    @Test
    void require_on_command_sends_error_when_extractor_throws() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("cmd") {{
            require(FAILING_EXTRACTOR).execute((ctx, s) -> ctx.sendMessage("executed"));
        }});

        TestCommandContext ctx = runner.execute("cmd");

        assertThat(ctx.messages()).containsExactly("prerequisite failed");
    }

    @Test
    void default_prerequisite_failure_matches_the_permission_message() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("cmd") {{
            addPrerequisite(ctx -> {
                throw new CommandPrerequisiteException();
            });
            execute(ctx -> ctx.sendMessage("executed"));
        }});

        TestCommandContext ctx = runner.execute("cmd");

        assertThat(ctx.messages()).containsExactly("You do not have permission to execute this command.");
    }

    @Test
    void require_on_argument_runs_execute_with_arg_and_extracted_value() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("cmd") {{
            argument(new CommonStringArgument<>("name")).require(SUCCEEDING_EXTRACTOR)
                                                        .execute((ctx, name, s) -> ctx.sendMessage(name + ":" + s));
        }});

        TestCommandContext ctx = runner.execute("cmd hello");

        assertThat(ctx.messages()).containsExactly("hello:extracted");
    }

    @Test
    void require_on_argument_sends_error_when_extractor_throws() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("cmd") {{
            argument(new CommonStringArgument<>("name")).require(FAILING_EXTRACTOR)
                                                        .execute((ctx, name, s) -> ctx.sendMessage("executed"));
        }});

        TestCommandContext ctx = runner.execute("cmd hello");

        assertThat(ctx.messages()).containsExactly("prerequisite failed");
    }

    @Test
    void require_on_two_arguments_passes_both_args_and_extracted_value() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("cmd") {{
            argument(new CommonStringArgument<>("a"),
                     new CommonIntegerArgument<>("b")).require(SUCCEEDING_EXTRACTOR)
                                                      .execute((ctx,
                                                                a,
                                                                b,
                                                                s) -> ctx.sendMessage(a + ":" + b + ":" + s));
        }});

        TestCommandContext ctx = runner.execute("cmd hello 42");

        assertThat(ctx.messages()).containsExactly("hello:42:extracted");
    }

    @Test
    void require_child_inherits_extractor_as_prerequisite() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("cmd") {{
            require(FAILING_EXTRACTOR).child(new TestCommand("sub") {{
                execute(ctx -> ctx.sendMessage("executed"));
            }});
        }});

        TestCommandContext ctx = runner.execute("cmd sub");

        assertThat(ctx.messages()).containsExactly("prerequisite failed");
    }

    @Test
    void require_child_executes_when_extractor_succeeds() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("cmd") {{
            require(SUCCEEDING_EXTRACTOR).child(new TestCommand("sub") {{
                execute(ctx -> ctx.sendMessage("executed"));
            }});
        }});

        TestCommandContext ctx = runner.execute("cmd sub");

        assertThat(ctx.messages()).containsExactly("executed");
    }

    @Test
    void require_on_argument_child_typed_factory_receives_argument() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("cmd") {{
            argument(new CommonStringArgument<>("key")).require(SUCCEEDING_EXTRACTOR)
                                                       .child(key -> new TestCommand("get") {{
                                                           execute(ctx -> ctx.sendMessage("get:" + key.get()));
                                                       }});
        }});

        TestCommandContext ctx = runner.execute("cmd difficulty get");

        assertThat(ctx.messages()).containsExactly("get:difficulty");
    }

    @Test
    void require_on_argument_child_inherits_prerequisite_from_extractor() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("cmd") {{
            argument(new CommonStringArgument<>("key")).require(FAILING_EXTRACTOR)
                                                       .child(key -> new TestCommand("get") {{
                                                           execute(ctx -> ctx.sendMessage("executed"));
                                                       }});
        }});

        TestCommandContext ctx = runner.execute("cmd difficulty get");

        assertThat(ctx.messages()).containsExactly("prerequisite failed");
    }
}
