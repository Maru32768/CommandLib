package dev.marulabs.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Commands and branches shown by a predicate ({@link CommonCommand#visibleWhen}).
 */
class VisibilityTest {
    private final AtomicBoolean running = new AtomicBoolean();

    private TestCommand game() {
        return new TestCommand("game") {{
            addChildren(new TestCommand("start") {{
                addAliases("begin");
                visibleWhen(ctx -> !running.get());
                execute(ctx -> running.set(true));
            }}, new TestCommand("stop") {{
                visibleWhen(ctx -> running.get());
                execute(ctx -> running.set(false));
            }}, new TestCommand("kick") {{
                argument(new CommonStringArgument<>("player")).visibleWhen(ctx -> ctx.getActor()
                                                                                     .isOperator())
                                                              .execute((player,
                                                                        ctx) -> ctx.sendMessage("kicked "
                                                                                + player));
                execute(ctx -> ctx.sendMessage("kick menu"));
            }});
        }};
    }

    private static TestCommandSource player(boolean operator) {
        return new TestCommandSource("Alex", UUID.randomUUID(), CommandActorType.PLAYER, operator, Set.of()) {
            @Override
            public boolean hasPermission(String permission) {
                return true;
            }
        };
    }

    @Test
    void a_predicate_reads_the_commands_provided_values_and_hides_when_it_throws() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            provide(String.class, ctx -> "host");
            addChildren(new TestCommand("start") {{
                visibleWhen(ctx -> ctx.get(String.class)
                                      .equals("host"));
                execute(ctx -> ctx.sendMessage("started"));
            }}, new TestCommand("broken") {{
                visibleWhen(ctx -> {
                    throw new IllegalStateException("broken predicate");
                });
                execute(ctx -> ctx.sendMessage("broken ran"));
            }});
        }}, player(false));

        assertThat(runner.visibleChildren("game")).contains("start")
                                                  .doesNotContain("broken");
        assertThat(runner.execute("game start")
                         .messages()).containsExactly("started");
        assertThatThrownBy(() -> runner.execute("game broken")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void the_tree_shows_only_what_the_predicate_allows() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(game(), player(false));

        assertThat(runner.visibleChildren("game")).contains("start", "begin", "kick")
                                                  .doesNotContain("stop");
        runner.execute("game start");
        assertThat(running).isTrue();
        assertThat(runner.visibleChildren("game")).contains("stop")
                                                  .doesNotContain("start", "begin");
        assertThatThrownBy(() -> runner.execute("game start")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("game begin")).isInstanceOf(CommandSyntaxException.class);
        runner.execute("game stop");
        assertThat(running).isFalse();
    }

    @Test
    void a_branch_is_hidden_and_rejected_by_its_predicate() throws Exception {
        TestCommandRunner member = new TestCommandRunner(game(), player(false));
        TestCommandRunner operator = new TestCommandRunner(game(), player(true));

        assertThat(member.visibleChildren("game", "kick")).doesNotContain("player");
        assertThatThrownBy(() -> member.execute("game kick Steve")).isInstanceOf(CommandSyntaxException.class);
        assertThat(operator.visibleChildren("game", "kick")).contains("player");
        assertThat(operator.execute("game kick Steve")
                           .messages()).containsExactly("kicked Steve");
    }

    @Test
    void help_lists_only_what_the_predicate_allows() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(game(), player(false));

        assertThat(runner.execute("game")
                         .messages()).anyMatch(x -> x.contains("start"))
                                     .noneMatch(x -> x.contains("stop"));
        assertThat(runner.execute("game kick help")
                         .messages()).noneMatch(x -> x.contains("<player>"));
    }

    @Test
    void the_runner_rejects_a_command_hidden_after_the_input_was_parsed() {
        TestCommandSource source = player(false);
        TestCommandRunner runner = new TestCommandRunner(game(), source);
        CommandDispatcher<TestCommandSource> dispatcher = new CommandDispatcher<>(runner.root());
        ParseResults<TestCommandSource> parsed = dispatcher.parse("game start", source);
        running.set(true);

        assertThatThrownBy(() -> dispatcher.execute(parsed)).isInstanceOf(CommandSyntaxException.class)
                                                            .hasMessageContaining("Unknown command");
        assertThat(running).isTrue();
    }

    @Test
    void the_runner_rejects_a_branch_or_a_parent_hidden_after_the_input_was_parsed() {
        AtomicBoolean hidden = new AtomicBoolean();
        TestCommand game = new TestCommand("game") {{
            addChildren(new TestCommand("kick") {{
                argument(new CommonStringArgument<>("player")).visibleWhen(ctx -> !hidden.get())
                                                              .execute((player,
                                                                        ctx) -> ctx.sendMessage("kicked "
                                                                                + player));
            }}, new TestCommand("admin") {{
                visibleWhen(ctx -> !hidden.get());
                addChildren(new TestCommand("reset") {{
                    execute(ctx -> ctx.sendMessage("reset"));
                }});
            }});
        }};
        TestCommandSource source = player(false);
        TestCommandRunner runner = new TestCommandRunner(game, source);
        CommandDispatcher<TestCommandSource> dispatcher = new CommandDispatcher<>(runner.root());
        ParseResults<TestCommandSource> kick = dispatcher.parse("game kick Steve", source);
        ParseResults<TestCommandSource> reset = dispatcher.parse("game admin reset", source);
        hidden.set(true);

        assertThatThrownBy(() -> dispatcher.execute(kick)).isInstanceOf(CommandSyntaxException.class)
                                                          .hasMessageContaining("Incorrect argument");
        assertThatThrownBy(() -> dispatcher.execute(reset)).isInstanceOf(CommandSyntaxException.class)
                                                           .hasMessageContaining("Unknown command");
    }
}
