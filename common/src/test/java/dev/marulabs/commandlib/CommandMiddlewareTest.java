package dev.marulabs.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import dev.marulabs.commandlib.command.CommandMiddleware;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandMiddlewareTest {
    private static CommandMiddleware<TestCommandContext> around(List<String> log, String name) {
        return (ctx, next) -> {
            log.add(name + " before");
            return next.proceed()
                       .whenComplete((x, e) -> log.add(name + " after"));
        };
    }

    @Test
    void middlewares_wrap_the_executor_in_order_with_the_parent_outermost() throws Exception {
        List<String> log = new ArrayList<>();
        TestCommand child = new TestCommand("child") {{
            addMiddleware(around(log, "child"));
            execute(ctx -> log.add("execute"));
        }};
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("root") {{
            addMiddleware(around(log, "first"));
            addMiddleware(around(log, "second"));
            addChildren(child);
        }});

        runner.dispatch("root child");

        assertThat(log).containsExactly("first before",
                                        "second before",
                                        "child before",
                                        "execute",
                                        "child after",
                                        "second after",
                                        "first after");
    }

    @Test
    void middlewares_apply_to_argument_branches() throws Exception {
        List<String> log = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("say") {{
            addMiddleware(around(log, "m"));
            argument(new CommonStringArgument<>("word")).execute((ctx, word) -> log.add(word));
        }});

        runner.dispatch("say hi");

        assertThat(log).containsExactly("m before", "hi", "m after");
    }

    @Test
    void a_child_can_disable_the_parent_middlewares() throws Exception {
        List<String> log = new ArrayList<>();
        TestCommand child = new TestCommand("child") {{
            disableParentMiddleware();
            execute(ctx -> log.add("execute"));
        }};
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("root") {{
            addMiddleware(around(log, "root"));
            addChildren(child);
        }});

        runner.dispatch("root child");

        assertThat(log).containsExactly("execute");
    }

    @Test
    void the_generated_help_skips_the_middlewares() throws Exception {
        List<String> log = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("root") {{
            addMiddleware(around(log, "root"));
            addChildren(new TestCommand("child") {{
                execute(ctx -> {
                });
            }});
        }});

        runner.dispatch("root");

        assertThat(log).isEmpty();
    }

    @Test
    void a_middleware_fails_the_command_with_a_prerequisite_exception() {
        List<String> log = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("run") {{
            addMiddleware((ctx, next) -> {
                throw new CommandPrerequisiteException("not now");
            });
            execute(ctx -> log.add("execute"));
        }});

        assertThatThrownBy(() -> runner.dispatch("run")).isInstanceOf(CommandSyntaxException.class)
                                                        .hasMessage("not now");
        assertThat(log).isEmpty();
    }

    @Test
    void a_middleware_skips_the_executor_by_not_proceeding() throws Exception {
        List<String> log = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("run") {{
            addMiddleware((ctx, next) -> {
                ctx.setResult(7);
                return CompletableFuture.completedFuture(null);
            });
            execute(ctx -> log.add("execute"));
        }});

        assertThat(runner.dispatch("run")).isEqualTo(7);
        assertThat(log).isEmpty();
    }

    @Test
    void a_middleware_sets_the_result_after_the_executor() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("run") {{
            addMiddleware((ctx, next) -> next.proceed()
                                             .thenRun(() -> ctx.setResult(ctx.getResult() * 10)));
            execute(ctx -> ctx.setResult(4));
        }});

        assertThat(runner.dispatch("run")).isEqualTo(40);
    }

    @Test
    void a_middleware_maps_an_exception_to_a_failure_message() {
        List<Throwable> uncaught = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("run") {{
            addMiddleware((ctx, next) -> next.proceed()
                                             .exceptionally(e -> {
                                                 throw new CompletionException(new CommandPrerequisiteException("The database is down."));
                                             }));
            addUncaughtExceptionHandler((ctx, e) -> uncaught.add(e));
            execute(ctx -> {
                throw new IllegalStateException("connection refused");
            });
        }});

        assertThatThrownBy(() -> runner.dispatch("run")).isInstanceOf(CommandSyntaxException.class)
                                                        .hasMessage("The database is down.");
        assertThat(uncaught).isEmpty();
    }

    @Test
    void an_exception_of_a_middleware_is_reported_as_an_unexpected_error() {
        List<Throwable> uncaught = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("run") {{
            addMiddleware((ctx, next) -> {
                throw new IllegalStateException("broken");
            });
            addUncaughtExceptionHandler((ctx, e) -> uncaught.add(e));
            execute(ctx -> {
            });
        }});

        assertThatThrownBy(() -> runner.dispatch("run")).isInstanceOf(CommandSyntaxException.class)
                                                        .hasMessageContaining("command.failed");
        assertThat(uncaught).singleElement()
                            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void preprocesses_and_middlewares_run_in_the_order_they_are_added() throws Exception {
        List<String> log = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("run") {{
            addMiddleware(around(log, "m"));
            addPreprocess(ctx -> {
                log.add("preprocess");
                return true;
            });
            execute(ctx -> log.add("execute"));
        }});

        runner.dispatch("run");

        assertThat(log).containsExactly("m before", "preprocess", "execute", "m after");
    }

    @Test
    void a_middleware_proceeding_from_another_thread_runs_the_executor_on_the_sync_thread() throws Exception {
        FakeScheduler scheduler = new FakeScheduler();
        List<String> log = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("run") {{
            // An async check, such as a permission lookup, before the executor.
            addMiddleware((ctx, next) -> ctx.supplyAsync(() -> true)
                                            .thenComposeAsync(ok -> next.proceed(), ctx.asyncExecutor()));
            execute(ctx -> log.add("execute on sync thread: " + scheduler.isSyncThread()));
        }}, scheduler);

        assertThat(runner.dispatch("run")).isEqualTo(1);
        assertThat(log).isEmpty();

        scheduler.runAll();
        scheduler.runAll();

        assertThat(log).containsExactly("execute on sync thread: true");
    }
}
