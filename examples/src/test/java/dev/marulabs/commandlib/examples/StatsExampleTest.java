package dev.marulabs.commandlib.examples;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandScheduler;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import dev.marulabs.commandlib.annotation.AnnotatedCommands;
import dev.marulabs.commandlib.examples.stats.AnnotatedStatsCommand;
import dev.marulabs.commandlib.examples.stats.BuilderStatsCommand;
import dev.marulabs.commandlib.examples.stats.FluentStatsCommand;
import dev.marulabs.commandlib.examples.stats.KotlinStatsCommandKt;
import dev.marulabs.commandlib.examples.stats.StatsStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class StatsExampleTest {
    private static final StatsStore STORE = String::length;

    @Test
    void every_style_gives_the_same_tree_help_and_results() {
        StyleParity.assertSameInEveryStyle("stats.snap",
                                           () -> new FluentStatsCommand(STORE),
                                           () -> BuilderStatsCommand.create(STORE),
                                           () -> AnnotatedCommands.parse(new AnnotatedStatsCommand(STORE)),
                                           () -> KotlinStatsCommandKt.kotlinStatsCommand(STORE),
                                           t -> {
                                               t.run("stats help");
                                               t.run("stats Steve");
                                           });
    }

    /**
     * Queues the tasks of both executors, so the test sees the command before the query, and that the query runs on
     * the async executor and the reply on the sync one.
     */
    @ParameterizedTest
    @ValueSource(strings = {"fluent", "builder", "annotation", "kotlin"})
    void queries_on_the_async_executor_and_replies_on_the_sync_executor(String style) {
        List<String> log = new ArrayList<>();
        Queue<Runnable> tasks = new ArrayDeque<>();
        CommandScheduler scheduler = CommandScheduler.of(task -> tasks.add(() -> {
            log.add("sync");
            task.run();
        }), task -> tasks.add(() -> {
            log.add("async");
            task.run();
        }));
        StatsStore store = player -> {
            log.add("query");
            return 3;
        };
        Function<StatsStore, Command> command = commands(style);

        FakeSender sender = FakeSender.player("Admin");
        try (CommandTester tester = CommandTester.builder()
                                                 .command(() -> command.apply(store))
                                                 .permissionPrefix(StyleParity.PREFIX)
                                                 .scheduler(scheduler)
                                                 .build()) {
            tester.execute("stats Steve", sender);
            assertThat(sender.getSentMessageTexts()).isEmpty();

            while (!tasks.isEmpty()) {
                tasks.poll()
                     .run();
                if (!sender.getSentMessageTexts()
                           .isEmpty()
                        && !log.contains("replied")) {
                    log.add("replied");
                }
            }
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Steve has 3 kills");
        // The query ran in an async task, and the reply was sent by the sync task that ran after it.
        assertThat(log.get(log.indexOf("query") - 1)).isEqualTo("async");
        assertThat(log.get(log.indexOf("replied") - 1)).isEqualTo("sync");
        assertThat(log.indexOf("query")).isLessThan(log.indexOf("replied"));
    }

    private static Function<StatsStore, Command> commands(String style) {
        switch (style) {
            case "fluent":
                return FluentStatsCommand::new;
            case "builder":
                return BuilderStatsCommand::create;
            case "annotation":
                return store -> AnnotatedCommands.parse(new AnnotatedStatsCommand(store));
            default:
                return KotlinStatsCommandKt::kotlinStatsCommand;
        }
    }
}
