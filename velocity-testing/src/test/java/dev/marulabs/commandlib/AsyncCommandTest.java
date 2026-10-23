package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.StringArgument;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncCommandTest {
    private static Command loadCommand() {
        return new Command("load") {{
            argument(new StringArgument("name")).executeAsync((ctx, name) -> ctx.supplyAsync(() -> name.length())
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
    void a_follow_up_after_execute_returned_still_sees_the_proxy() {
        Queue<Runnable> asyncTasks = new ArrayDeque<>();
        try (CommandTester tester = CommandTester.builder()
                                                 .command(new Command("version") {{
                                                     executeAsync(ctx -> ctx.supplyAsync(() -> ctx.getProxy()
                                                                                                  .getVersion()
                                                                                                  .getName())
                                                                            .thenAccept(ctx::sendMessage));
                                                 }})
                                                 .permissionPrefix("test")
                                                 .scheduler(CommandScheduler.of(Runnable::run, asyncTasks::add))
                                                 .build()) {
            FakeSender sender = FakeSender.player("Steve");

            tester.execute("version", sender);
            asyncTasks.forEach(Runnable::run);

            assertThat(sender.getSentMessageTexts()).containsExactly("Velocity");
        }
    }
}
