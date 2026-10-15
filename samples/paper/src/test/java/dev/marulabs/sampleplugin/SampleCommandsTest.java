package dev.marulabs.sampleplugin;

import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import dev.marulabs.commandlib.annotation.AnnotatedCommands;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the sample commands with paper-testing, without a server.
 */
class SampleCommandsTest {
    @Test
    void increment_defaults_the_count_to_one() {
        FakeSender console = FakeSender.console();
        try (CommandTester tester = new CommandTester(IncrementCommand::new, "sample.command")) {
            tester.execute("increment apples", console);
            tester.execute("increment apples 5", console);
        }
        assertThat(console.getSentMessageTexts()).containsExactly("apples = 1", "apples = 6");
    }

    @Test
    void math_sum_takes_any_number_of_values() {
        FakeSender console = FakeSender.console();
        try (CommandTester tester = new CommandTester(MathCommand::new, "sample.command")) {
            tester.execute("math sum 1 2 3 4", console);
        }
        assertThat(console.getSentMessageTexts()).containsExactly("10");
    }

    @Test
    void reset_runs_on_the_second_input() {
        FakeSender console = FakeSender.console();
        try (CommandTester tester = new CommandTester(ResetCommand::new, "sample.command")) {
            tester.execute("reset", console);
            assertThat(console.getSentMessageTexts()).doesNotContain("Reset.");
            tester.execute("reset", console);
        }
        assertThat(console.getSentMessageTexts()).last()
                                                 .isEqualTo("Reset.");
    }

    @Test
    void lookup_completes_after_the_async_work() {
        FakeSender console = FakeSender.console();
        try (CommandTester tester = new CommandTester(() -> new LookupCommand(Logger.getLogger("test")),
                                                      "sample.command")) {
            tester.execute("lookup steve", console);
        }
        assertThat(console.getSentMessageTexts()).containsExactly("steve -> STEVE");
    }

    @Test
    void arena_takes_its_arguments_and_flag_from_the_annotations() {
        FakeSender console = FakeSender.console();
        try (CommandTester tester = new CommandTester(() -> AnnotatedCommands.parse(new ArenaCommand()),
                                                      "sample.command")) {
            tester.execute("arena create lobby", console);
            tester.execute("arena create lobby 8", console);
            tester.execute("arena create -f lobby 8", console);
            tester.execute("arena list", console);
        }
        assertThat(console.getSentMessageTexts()).containsExactly("Created lobby with 16 slots",
                                                                  "lobby exists. Add -f to replace it.",
                                                                  "Created lobby with 8 slots",
                                                                  "Arenas: {lobby=8}");
    }

    @Test
    void the_builder_command_has_its_alias() {
        try (CommandTester tester = new CommandTester(FeedCommand::create, "sample.command")) {
            assertThat(Arrays.asList(tester.commandTree()
                                           .split("\n"))).anyMatch(line -> line.contains("eat"));
        }
    }
}
