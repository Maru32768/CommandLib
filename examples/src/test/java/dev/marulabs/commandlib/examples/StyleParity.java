package dev.marulabs.commandlib.examples;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.ExampleSnapshots;
import dev.marulabs.commandlib.FakeSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the same script against each style of a use case and checks that every style gives the same transcript: the
 * command tree, then what each input returns and sends. The transcript is compared with a snapshot.
 */
final class StyleParity {
    static final String PREFIX = "example.command";

    private StyleParity() {
    }

    static void assertSameInEveryStyle(String snapshot,
                                       Supplier<Command> fluent,
                                       Supplier<Command> builder,
                                       Supplier<Command> annotated,
                                       Supplier<Command> kotlin,
                                       Consumer<Transcript> script) {
        Map<String, Supplier<Command>> styles = new LinkedHashMap<>();
        styles.put("fluent", fluent);
        styles.put("builder", builder);
        styles.put("annotation", annotated);
        styles.put("kotlin", kotlin);

        String expected = transcript(fluent, script);
        styles.forEach((style, command) -> assertThat(transcript(command, script)).as(style)
                                                                                  .isEqualTo(expected));
        ExampleSnapshots.assertMatches(snapshot, expected);
    }

    private static String transcript(Supplier<Command> command, Consumer<Transcript> script) {
        // The tester creates the command, since PlayerArgument can only be created while it stubs NMS.
        try (CommandTester tester = new CommandTester(command, PREFIX)) {
            tester.withFakePlayer((Player) FakeSender.player("Steve")
                                                     .asSender());
            tester.withFakePlayer((Player) FakeSender.player("Alex")
                                                     .asSender());
            Transcript transcript = new Transcript(tester);
            transcript.line("# Tree");
            transcript.line(tester.commandTree());
            transcript.line("# Inputs");
            script.accept(transcript);
            return String.join("\n", transcript.lines);
        }
    }

    static final class Transcript {
        final FakeSender admin = FakeSender.player("Admin");
        private final CommandTester tester;
        private final List<String> lines = new ArrayList<>();

        private Transcript(CommandTester tester) {
            this.tester = tester;
        }

        void line(String line) {
            lines.add(line);
        }

        void run(String input) {
            run(input, admin);
        }

        /**
         * Adds the input, the result {@code /execute store result} would store, and the messages the sender got.
         */
        void run(String input, FakeSender sender) {
            int before = sender.getSentMessageTexts()
                               .size();
            String result;
            try {
                result = String.valueOf(tester.execute(input, sender));
            } catch (RuntimeException e) {
                result = "rejected: " + e.getMessage();
            }
            List<String> messages = sender.getSentMessageTexts();
            lines.add("/" + input + " -> " + result);
            messages.subList(before, messages.size())
                    .forEach(message -> lines.add("    " + message.replace("\n", "\n    ")));
        }

        void suggest(String input) {
            lines.add("suggest /" + input + " -> " + tester.suggest(input, admin));
        }
    }
}
