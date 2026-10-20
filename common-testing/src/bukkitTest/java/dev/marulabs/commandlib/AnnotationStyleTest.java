package dev.marulabs.commandlib;

import dev.marulabs.commandlib.annotation.AnnotatedCommands;
import org.bukkit.entity.Player;
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
        String annotated = describe(() -> AnnotatedCommands.parse(new AnnotatedArenaCommand()),
                                    INPUTS,
                                    "arena delete ",
                                    List.of("arena join lobby"));
        String fluent = describe(FluentArenaCommand::new, INPUTS, "arena delete ", List.of("arena join lobby"));

        assertThat(annotated).isEqualTo(fluent);
        Snapshots.assertMatches("annotation-arena.snap", annotated);
    }

    private static final List<String> GUILD_INPUTS = List.of("guild",
                                                             "guild help",
                                                             "guild red invite Steve Alex",
                                                             "guild red invite Steve Alex Steve Alex",
                                                             "guild red kick Steve",
                                                             "guild red kick Steve -s",
                                                             "guild red kick -s -l Steve",
                                                             "guild red kick -r spam Steve",
                                                             "guild red kick -r spam -b Alex Steve -l");

    @Test
    void annotated_and_fluent_guild_commands_have_the_same_help_permissions_and_behavior() {
        List<String> console = List.of("guild", "guild red kick Steve");
        String annotated = describe(() -> AnnotatedCommands.parser()
                                                           .registerMiddleware("audit", FluentGuildCommand.AUDIT)
                                                           .registerVisibility("players", FluentGuildCommand.PLAYERS)
                                                           .parse(new AnnotatedGuildCommand()),
                                    GUILD_INPUTS,
                                    "guild red kick -",
                                    console);
        String fluent = describe(FluentGuildCommand::new, GUILD_INPUTS, "guild red kick -", console);

        assertThat(annotated).isEqualTo(fluent);
        Snapshots.assertMatches("annotation-guild.snap", annotated);
    }

    /**
     * The command tree, the permissions, the suggestions and what each input sends.
     */
    private static String describe(Supplier<Command> command,
                                   List<String> inputs,
                                   String suggestion,
                                   List<String> consoleInputs) {
        List<String> lines = new ArrayList<>();
        // The tester creates the command, since Paper's arguments can only be created while it stubs the server.
        List<Command> created = new ArrayList<>();
        try (CommandTester tester = new CommandTester(() -> {
            created.add(command.get());
            return created.get(0);
        }, "test.command")) {
            FakeSender admin = FakeSender.player("Admin");
            FakeSender console = FakeSender.console();
            tester.withFakePlayer((Player) FakeSender.player("Steve")
                                                     .asSender());
            tester.withFakePlayer((Player) FakeSender.player("Alex")
                                                     .asSender());

            lines.add("# Tree");
            lines.add(tester.commandTree());
            lines.add("# Permissions");
            lines.add(CommandDocs.of("test.command", created.get(0))
                                 .permissionsMarkdown());
            lines.add("# Suggestions of /" + suggestion.trim());
            lines.add(String.join(", ", tester.suggest(suggestion, admin)));
            lines.add("# Admin");
            for (String input : inputs) {
                lines.add("/" + input + " -> " + run(tester, input, admin));
            }
            lines.add("# Console");
            for (String input : consoleInputs) {
                lines.add("/" + input + " -> " + run(tester, input, console));
            }
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
