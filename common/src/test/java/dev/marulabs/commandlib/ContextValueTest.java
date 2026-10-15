package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.CommonStringArgument;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextValueTest {
    // A plugin's own sender type, mapped from the platform's sender.
    static final class GamePlayer {
        private final String name;

        GamePlayer(String name) {
            this.name = name;
        }

        String name() {
            return name;
        }
    }

    private static final class Word extends CommonStringArgument<TestCommandContext, Word> {
        Word(String name) {
            super(name);
        }
    }

    @Test
    void a_provider_maps_the_sender_once_per_execution() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        List<String> log = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            provide(GamePlayer.class, ctx -> {
                calls.incrementAndGet();
                return new GamePlayer(ctx.getActor()
                                         .getName());
            });
            addMiddleware((ctx, next) -> {
                log.add("middleware " + ctx.get(GamePlayer.class)
                                           .name());
                return next.proceed();
            });
            argument(new Word("arena")).execute((arena, ctx) -> log.add(ctx.get(GamePlayer.class)
                                                                           .name()
                    + " joins " + arena));
        }});

        runner.dispatch("game lobby");
        runner.dispatch("game hub");

        assertThat(log).containsExactly("middleware test", "test joins lobby", "middleware test", "test joins hub");
        assertThat(calls).hasValue(2);
    }

    @Test
    void registration_defaults_wrap_every_command_and_provide_values_it_does_not() throws Exception {
        List<String> log = new ArrayList<>();
        TestCommand command = new TestCommand("root") {{
            provide(String.class, ctx -> "root");
            confirm();
            addMiddleware((ctx, next) -> {
                log.add("command");
                return next.proceed();
            });
            addChildren(new TestCommand("free") {{
                disableParentMiddleware();
                execute(ctx -> log.add("free " + ctx.get(String.class) + " " + ctx.get(Integer.class)));
            }});
            execute(ctx -> log.add(ctx.get(String.class) + " " + ctx.get(Integer.class)));
        }};
        CommandDefaults.of(TestCommandContext.class)
                       .addMiddleware((ctx, next) -> {
                           log.add("registration");
                           return next.proceed();
                       })
                       .provide(String.class, ctx -> "registration")
                       .provide(Integer.class, ctx -> 1)
                       .applyTo(List.of(command));
        TestCommandRunner runner = new TestCommandRunner(command);

        // The registration's middleware runs before the confirmation, which fails the first execution.
        runner.executeAndGetResult("root");
        runner.dispatch("root");
        runner.dispatch("root free");

        assertThat(log).containsExactly("registration",
                                        "registration",
                                        "command",
                                        "root 1",
                                        "registration",
                                        "free root 1");
    }

    @Test
    void children_inherit_providers_and_replace_them_for_the_same_key() throws Exception {
        List<String> log = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("root") {{
            provide(String.class, ctx -> "root");
            provide(Integer.class, ctx -> 1);
            addChildren(new TestCommand("inherit") {{
                disableParentMiddleware();
                execute(ctx -> log.add(ctx.get(String.class) + " " + ctx.get(int.class)));
            }}, new TestCommand("replace") {{
                provide(String.class, ctx -> "child");
                execute(ctx -> log.add(ctx.get(String.class) + " " + ctx.get(Integer.class)));
            }});
            argument(new Word("word")).child(word -> new TestCommand("after") {{
                execute(ctx -> log.add(ctx.get(String.class) + " after " + word.get()));
            }});
        }});

        runner.dispatch("root inherit");
        runner.dispatch("root replace");
        runner.dispatch("root x after");

        assertThat(log).containsExactly("root 1", "child 1", "root after x");
    }

    @Test
    void middlewares_put_values_that_later_middlewares_and_the_executor_read() throws Exception {
        ContextKey<Long> started = ContextKey.of("started", Long.class);
        ContextKey<Long> finished = ContextKey.of("finished", Long.class);
        List<String> log = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("timed") {{
            provide(GamePlayer.class, ctx -> new GamePlayer("provided"));
            addMiddleware((ctx, next) -> {
                ctx.put(started, 10L);
                ctx.put(GamePlayer.class, new GamePlayer("put"));
                return next.proceed();
            });
            addMiddleware((ctx, next) -> {
                log.add("started " + ctx.get(started) + ", finished " + ctx.find(finished)
                                                                           .isPresent());
                return next.proceed();
            });
            // The named key is not the key of the type.
            execute(ctx -> log.add(ctx.get(GamePlayer.class)
                                      .name()
                    + " " + ctx.get(started) + " " + ctx.find(Long.class)
                                                        .isPresent()));
        }});

        runner.dispatch("timed");

        assertThat(log).containsExactly("started 10, finished false", "put 10 false");
    }

    @Test
    void a_provider_failure_fails_the_command_with_its_message() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            provide(GamePlayer.class, ctx -> {
                throw new CommandPrerequisiteException("You are not in a game.");
            });
            // The typed executor cannot throw the checked exception, but the provider's still fails the command.
            argument(new Word("arena")).execute((arena, ctx) -> ctx.sendMessage(ctx.get(GamePlayer.class)
                                                                                   .name()));
            addChildren(new TestCommand("leave") {{
                require(ctx -> ctx.get(GamePlayer.class)).execute((player,
                                                                   ctx) -> ctx.sendMessage(player.name()));
            }});
        }});

        assertThat(runner.execute("game lobby")
                         .messages()).containsExactly("You are not in a game.");
        assertThat(runner.executeAndGetResult("game lobby")).isZero();
        assertThat(runner.execute("game leave")
                         .messages()).containsExactly("You are not in a game.");
    }

    @Test
    void a_missing_value_or_a_provider_reading_its_own_value_is_an_error() throws Exception {
        List<String> log = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("root") {{
            provide(Integer.class, ctx -> ctx.get(Integer.class) + 1);
            addChildren(new TestCommand("missing") {{
                execute(ctx -> {
                    log.add(String.valueOf(ctx.find(GamePlayer.class)
                                              .isPresent()));
                    assertThatThrownBy(() -> ctx.get(GamePlayer.class)).isInstanceOf(IllegalStateException.class)
                                                                       .hasMessage("No value for "
                                                                               + GamePlayer.class.getName()
                                                                               + " in this command execution; provide one with provide(...) on the command or put(...) in a middleware");
                });
            }}, new TestCommand("cycle") {{
                execute(ctx -> assertThatThrownBy(() -> ctx.get(Integer.class)).hasMessage("The provider of java.lang.Integer reads its own value"));
            }});
        }});

        runner.dispatch("root missing");
        runner.dispatch("root cycle");

        assertThat(log).containsExactly("false");
    }

    @Test
    void prerequisites_argument_defaults_and_suggestions_read_provided_values() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("arena") {{
            provide(GamePlayer.class, ctx -> new GamePlayer("Steve"));
            addPrerequisite(ctx -> {
                if (ctx.get(GamePlayer.class)
                       .name()
                       .isEmpty()) {
                    throw new CommandPrerequisiteException("No name.");
                }
            });
            argument(new Word("owner").optional(ctx -> ctx.get(GamePlayer.class)
                                                          .name())
                                      .setSuggestionProvider(sb -> sb.suggest(sb.getContext()
                                                                                .get(GamePlayer.class)
                                                                                .name()))).execute((owner,
                                                                                                    ctx) -> ctx.sendMessage("owner "
                                                                                                            + owner));
        }});

        assertThat(runner.execute("arena")
                         .messages()).containsExactly("owner Steve");
        assertThat(runner.suggest("arena ")).containsExactly("help", "Steve");
    }

    @Test
    void context_keys_are_equal_by_name_and_boxed_type() {
        assertThat(ContextKey.of(int.class)).isEqualTo(ContextKey.of(Integer.class));
        assertThat(ContextKey.of(int.class)
                             .type()).isEqualTo(Integer.class);
        assertThat(ContextKey.of("a", String.class)).isEqualTo(ContextKey.of("a", String.class))
                                                    .isNotEqualTo(ContextKey.of(String.class))
                                                    .isNotEqualTo(ContextKey.of("b", String.class))
                                                    .hasToString("a (java.lang.String)");
        assertThat(ContextKey.of(String.class)).hasToString("java.lang.String");
        assertThatThrownBy(() -> ContextKey.of("", String.class)).isInstanceOf(IllegalArgumentException.class);
    }
}
