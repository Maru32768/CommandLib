package dev.marulabs.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.argument.CommonLiteralArgument;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandResultTest {
    @Test
    void result_defaults_to_one() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("run") {{
            execute(ctx -> {
            });
        }});

        assertThat(runner.dispatch("run")).isEqualTo(1);
    }

    @Test
    void executor_sets_the_result() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("count") {{
            argument(new CommonStringArgument<>("word")).execute((ctx, word) -> ctx.setResult(word.length()));
        }});

        assertThat(runner.dispatch("count hello")).isEqualTo(5);
    }

    @Test
    void failure_is_thrown_to_the_platform_with_its_messages_instead_of_being_sent() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("run") {{
            execute(ctx -> {
                ctx.sendMessage("before");
                throw new CommandPrerequisiteException("not now", "try later");
            });
        }});

        assertThatThrownBy(() -> runner.dispatch("run")).isInstanceOf(CommandSyntaxException.class)
                                                        .hasMessage("not now\ntry later");
        // Messages sent before the failure still reach the sender directly.
        assertThat(TestCommandContext.latest()
                                     .messages()).containsExactly("before");
    }

    @Test
    void ctx_fail_fails_a_typed_executor_like_a_prerequisite_exception() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("count") {{
            argument(new CommonStringArgument<>("word")).execute((ctx, word) -> {
                if (word.equals("none")) {
                    throw ctx.fail("nothing to count", "try another word");
                }
                ctx.setResult(word.length());
            });
            addChildren(new TestCommand("player") {{
                execute(ctx -> {
                    throw ctx.fail(Caption.REQUIRE_PLAYER);
                });
            }});
        }});

        assertThatThrownBy(() -> runner.dispatch("count none")).isInstanceOf(CommandSyntaxException.class)
                                                               .hasMessage("nothing to count\ntry another word");
        assertThat(runner.executeAndGetResult("count none")).isZero();
        assertThat(TestCommandContext.latest()
                                     .messages()).containsExactly("nothing to count", "try another word");
        assertThat(runner.executeAndGetResult("count player")).isZero();
        assertThat(TestCommandContext.latest()
                                     .messages()).containsExactly("This command can only be executed by a player.");
        assertThat(runner.dispatch("count four")).isEqualTo(4);
    }

    @Test
    void parse_failure_is_thrown_to_the_platform() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("go") {{
            argument(new CommonLiteralArgument<>("dir", List.of("north"))).execute((ctx, dir) -> {
            });
        }});

        assertThatThrownBy(() -> runner.dispatch("go south")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void preprocess_returning_false_fails_with_its_messages() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("run") {{
            addPreprocess(ctx -> {
                ctx.sendFailure("not in a game");
                return false;
            });
            execute(ctx -> ctx.sendMessage("unreachable"));
        }});

        assertThatThrownBy(() -> runner.dispatch("run")).isInstanceOf(CommandSyntaxException.class)
                                                        .hasMessage("not in a game");
    }

    @Test
    void preprocess_messages_are_sent_when_it_passes() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("run") {{
            addPreprocess(ctx -> {
                ctx.sendMessage("checked");
                return true;
            });
            execute(ctx -> ctx.sendMessage("ran"));
        }});

        assertThat(runner.execute("run")
                         .messages()).containsExactly("checked", "ran");
    }

    @Test
    void failure_without_a_message_returns_zero_instead_of_throwing() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("run") {{
            addPreprocess(ctx -> false);
            execute(ctx -> ctx.sendMessage("unreachable"));
        }});

        assertThat(runner.dispatch("run")).isZero();
    }

    @Test
    void unexpected_exception_fails_with_the_generic_messages() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("boom") {{
            execute(ctx -> {
                throw new IllegalStateException("boom");
            });
        }});

        assertThatThrownBy(() -> runner.dispatch("boom")).isInstanceOf(CommandSyntaxException.class)
                                                         .hasMessage("command.failed\nCheck the console for details.");
    }
}
