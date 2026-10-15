package dev.marulabs.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AsyncExecutionTest {
    private final FakeScheduler scheduler = new FakeScheduler();
    private final List<Throwable> uncaught = new ArrayList<>();

    private TestCommandRunner runner(TestCommand command) {
        command.addUncaughtExceptionHandler((e, ctx) -> uncaught.add(e));
        return new TestCommandRunner(command, scheduler);
    }

    @Test
    void work_runs_async_and_the_follow_up_on_the_sync_thread() throws Exception {
        List<String> threads = new ArrayList<>();
        TestCommandRunner runner = runner(new TestCommand("load") {{
            executeAsync(ctx -> ctx.supplyAsync(() -> {
                threads.add("work on sync thread: " + scheduler.isSyncThread());
                return "data";
            })
                                   .thenAccept(data -> {
                                       threads.add("follow-up on sync thread: " + scheduler.isSyncThread());
                                       ctx.sendSuccess(data);
                                   }));
        }});

        // Brigadier gets the result at once; the command has not finished yet.
        assertThat(runner.dispatch("load")).isEqualTo(1);
        TestCommandContext ctx = TestCommandContext.latest();
        assertThat(ctx.messages()).isEmpty();

        scheduler.runAsyncTasks();
        assertThat(ctx.messages()).isEmpty();

        scheduler.runSyncTasks();
        assertThat(ctx.messages()).containsExactly("data");
        assertThat(threads).containsExactly("work on sync thread: false", "follow-up on sync thread: true");
    }

    @Test
    void the_dispatch_returns_the_result_set_before_the_stage_was_returned() throws Exception {
        TestCommandRunner runner = runner(new TestCommand("count") {{
            executeAsync(ctx -> {
                ctx.setResult(3);
                return ctx.runAsync(() -> {
                })
                          .thenRun(() -> ctx.setResult(99));
            });
        }});

        assertThat(runner.dispatch("count")).isEqualTo(3);
        scheduler.runAll();
    }

    @Test
    void typed_branches_take_async_executors() throws Exception {
        TestCommandRunner runner = runner(new TestCommand("len") {{
            argument(new CommonStringArgument<>("word")).executeAsync((word, ctx) -> ctx.supplyAsync(word::length)
                                                                                        .thenAccept(ctx::sendMessage));
        }});

        runner.dispatch("len hello");
        scheduler.runAll();

        assertThat(TestCommandContext.latest()
                                     .messages()).containsExactly("5");
    }

    @Test
    void a_prerequisite_exception_after_the_dispatch_sends_its_message() throws Exception {
        TestCommandRunner runner = runner(new TestCommand("load") {{
            executeAsync(ctx -> ctx.supplyAsync(() -> null)
                                   .thenAccept(x -> {
                                       throw new java.util.concurrent.CompletionException(new CommandPrerequisiteException("No such player."));
                                   }));
        }});

        assertThat(runner.dispatch("load")).isEqualTo(1);
        scheduler.runAll();

        assertThat(TestCommandContext.latest()
                                     .messages()).containsExactly("No such player.");
        assertThat(uncaught).isEmpty();
    }

    @Test
    void an_unexpected_exception_after_the_dispatch_reaches_the_handlers_on_the_sync_thread() throws Exception {
        List<Boolean> handlerOnSyncThread = new ArrayList<>();
        TestCommandRunner runner = runner(new TestCommand("load") {{
            addUncaughtExceptionHandler((e, ctx) -> handlerOnSyncThread.add(scheduler.isSyncThread()));
            executeAsync(ctx -> CompletableFuture.runAsync(() -> {
                throw new IllegalStateException("disk full");
            }, ctx.asyncExecutor()));
        }});

        runner.dispatch("load");
        scheduler.runAsyncTasks();
        assertThat(uncaught).isEmpty();
        scheduler.runSyncTasks();

        assertThat(TestCommandContext.latest()
                                     .messages()).containsExactly("An unexpected error occurred trying to execute that command.",
                                                                  "Check the console for details.");
        assertThat(uncaught).singleElement()
                            .isInstanceOf(IllegalStateException.class);
        assertThat(handlerOnSyncThread).containsExactly(true);
    }

    @Test
    void a_timeout_reports_that_the_command_timed_out() throws Exception {
        CompletableFuture<Void> work = new CompletableFuture<>();
        TestCommandRunner runner = runner(new TestCommand("load") {{
            executeAsync(ctx -> work);
        }});

        runner.dispatch("load");
        // As orTimeout does, from another thread.
        Thread thread = new Thread(() -> work.completeExceptionally(new TimeoutException()));
        thread.start();
        thread.join();
        scheduler.runSyncTasks();

        assertThat(TestCommandContext.latest()
                                     .messages()).containsExactly("The command timed out.");
        assertThat(uncaught).singleElement()
                            .isInstanceOf(TimeoutException.class);
    }

    @Test
    void a_cancelled_command_reports_nothing() throws Exception {
        CompletableFuture<Void> work = new CompletableFuture<>();
        TestCommandRunner runner = runner(new TestCommand("load") {{
            executeAsync(ctx -> work);
        }});

        runner.dispatch("load");
        work.cancel(false);
        scheduler.runSyncTasks();

        assertThat(TestCommandContext.latest()
                                     .messages()).isEmpty();
        assertThat(uncaught).isEmpty();
    }

    @Test
    void a_stage_completed_before_the_dispatch_returns_behaves_like_a_sync_command() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("load") {{
            executeAsync(ctx -> ctx.supplyAsync(() -> "data")
                                   .thenAccept(data -> {
                                       ctx.setResult(5);
                                       throw new java.util.concurrent.CompletionException(new CommandPrerequisiteException("No data."));
                                   }));
        }});

        // With executors that run tasks at once, as the testers use by default, the failure reaches Brigadier.
        assertThatThrownBy(() -> runner.dispatch("load")).isInstanceOf(CommandSyntaxException.class)
                                                         .hasMessage("No data.");
    }

    @Test
    void a_middleware_post_hook_runs_when_the_async_work_completes() throws Exception {
        List<String> log = new ArrayList<>();
        TestCommandRunner runner = runner(new TestCommand("load") {{
            addMiddleware((ctx, next) -> next.proceed()
                                             .whenComplete((x, e) -> log.add("after, on sync thread: "
                                                     + scheduler.isSyncThread())));
            executeAsync(ctx -> ctx.runAsync(() -> log.add("work")));
        }});

        runner.dispatch("load");
        assertThat(log).isEmpty();
        scheduler.runAll();

        assertThat(log).containsExactly("work", "after, on sync thread: true");
    }

    @Test
    void the_executors_are_not_available_outside_an_execution() {
        TestCommandContext[] suggestionContext = new TestCommandContext[1];
        CommonStringArgument<TestCommandContext, ?> word = new CommonStringArgument<>("word");
        word.setSuggestionProvider(builder -> suggestionContext[0] = builder.getContext());
        TestCommandRunner runner = runner(new TestCommand("pick") {{
            argument(word).execute((w, ctx) -> {
            });
        }});

        runner.suggest("pick ");

        assertThatThrownBy(() -> suggestionContext[0].syncExecutor()).isInstanceOf(IllegalStateException.class);
    }
}
