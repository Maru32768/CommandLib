package dev.marulabs.commandlib;

import dev.marulabs.commandlib.annotation.AnnotatedCommands;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationStyleTest {
    private static final List<String> INPUTS = List.of("arena",
                                                       "arena create",
                                                       "arena create lobby2",
                                                       "arena create -f big 32",
                                                       "arena create huge 100",
                                                       "arena list -n 1",
                                                       "ar list",
                                                       "arena delete lobby",
                                                       "arena delete lobby",
                                                       "arena invite lobby Steve Alex",
                                                       "arena join lobby",
                                                       "arena join lobby Steve",
                                                       "arena stats lobby");

    @Test
    void annotated_and_fluent_arena_commands_have_the_same_help_permissions_and_behavior() {
        String annotated = describe(() -> AnnotatedCommands.parse(new AnnotatedArenaCommand()));
        String fluent = describe(FluentArenaCommand::new);

        assertThat(annotated).isEqualTo(fluent)
                             .contains("Created big with 32 slots (forced)",
                                       "Invited Steve, Alex to lobby",
                                       "Admin joined lobby",
                                       "Steve joined lobby",
                                       "This command can only be executed by a player.");
    }

    /**
     * The command tree, the permissions, the suggestions and what each input sends.
     */
    private static String describe(Supplier<Command> command) {
        List<String> lines = new ArrayList<>();
        List<Command> created = new ArrayList<>();
        try (CommandTester tester = new CommandTester(() -> {
            created.add(command.get());
            return created.get(0);
        }, "test.command")) {
            FakeSender admin = FakeSender.player("Admin");
            FakeSender console = FakeSender.console();
            tester.withPlayer(FakeSender.player("Steve"));
            tester.withPlayer(FakeSender.player("Alex"));

            lines.add("# Tree");
            lines.add(tester.commandTree());
            lines.add("# Permissions");
            lines.add(CommandDocs.of("test.command", created.get(0))
                                 .permissionsMarkdown());
            lines.add("# Suggestions of /arena delete");
            lines.add(String.join(", ", tester.suggest("arena delete ", admin)));
            lines.add("# Admin");
            for (String input : INPUTS) {
                lines.add("/" + input + " -> " + run(tester, input, admin));
            }
            lines.add("# Console");
            lines.add("/arena join lobby -> " + run(tester, "arena join lobby", console));
        }
        return String.join("\n", lines);
    }

    private static String run(CommandTester tester, String input, FakeSender sender) {
        int before = sender.getSentMessageTexts()
                           .size();
        String result;
        try {
            result = String.valueOf(tester.execute(input, sender));
        } catch (RuntimeException e) {
            result = e.getClass()
                      .getSimpleName()
                    + ": " + e.getMessage();
        }
        List<String> messages = sender.getSentMessageTexts();
        return result + "\n    " + String.join("\n    ", messages.subList(before, messages.size()));
    }
}
