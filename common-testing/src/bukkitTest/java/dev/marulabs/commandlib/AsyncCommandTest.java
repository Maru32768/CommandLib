package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.StringArgument;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncCommandTest {
    private static Command loadCommand() {
        return new Command("load") {{
            argument(new StringArgument("name")).executeAsync((name, ctx) -> ctx.supplyAsync(() -> name.length())
                                                                                .thenAccept(length -> ctx.sendSuccess(name
                                                                                        + " has " + length
                                                                                        + " letters")));
        }};
    }

    @Test
    void the_default_scheduler_completes_the_command_before_execute_returns() {
        try (CommandTester tester = new CommandTester(loadCommand(), "test")) {
            FakeSender sender = FakeSender.player("Steve");

            assertThat(tester.execute("load abc", sender)).isEqualTo(1);

            assertThat(sender.getSentMessageTexts()).containsExactly("abc has 3 letters");
        }
    }

    @Test
    void a_queued_async_executor_shows_the_state_before_the_work_finishes() {
        Queue<Runnable> asyncTasks = new ArrayDeque<>();
        try (CommandTester tester = CommandTester.builder()
                                                 .command(loadCommand())
                                                 .permissionPrefix("test")
                                                 .scheduler(CommandScheduler.of(Runnable::run, asyncTasks::add))
                                                 .build()) {
            FakeSender sender = FakeSender.player("Steve");

            assertThat(tester.execute("load abc", sender)).isEqualTo(1);
            assertThat(sender.getSentMessageTexts()).isEmpty();

            asyncTasks.forEach(Runnable::run);

            // The follow-up still reaches the sender after execute has returned.
            assertThat(sender.getSentMessageTexts()).containsExactly("abc has 3 letters");
        }
    }

    @Test
    void an_async_executor_on_another_thread_still_reaches_the_sender() throws Exception {
        ExecutorService async = Executors.newSingleThreadExecutor();
        try (CommandTester tester = CommandTester.builder()
                                                 .command(loadCommand())
                                                 .permissionPrefix("test")
                                                 .scheduler(CommandScheduler.of(Runnable::run, async))
                                                 .build()) {
            FakeSender sender = FakeSender.player("Steve");

            tester.execute("load abc", sender);
            async.shutdown();

            assertThat(async.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            assertThat(sender.getSentMessageTexts()).containsExactly("abc has 3 letters");
        } finally {
            async.shutdownNow();
        }
    }
}
