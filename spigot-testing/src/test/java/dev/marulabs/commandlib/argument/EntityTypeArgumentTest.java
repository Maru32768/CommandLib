package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.suggestion.Suggestion;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityTypeArgumentTest {
    private static Command command() {
        return new Command("entitytype") {{
            argument(new EntityTypeArgument("value")).execute((type, ctx) -> ctx.sendMessage(type.name()));
        }};
    }

    @Test
    void entity_type_is_resolved_by_registry_key() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(EntityTypeArgumentTest::command, "test.command")) {
            tester.execute("entitytype minecraft:zombie", sender);
            tester.execute("entitytype zombie", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("ZOMBIE", "ZOMBIE");
    }

    @Test
    void unknown_entity_type_sends_failure_message() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(EntityTypeArgumentTest::command, "test.command")) {
            tester.execute("entitytype unknown", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("unknown is not a known entity type");
    }

    @Test
    void suggestions_are_registry_keys_without_unknown() throws Exception {
        try (CommandTester tester = new CommandTester(EntityTypeArgumentTest::command, "test.command")) {
            var suggestions = tester.suggestions("entitytype ", FakeSender.player("Alice"))
                                    .get()
                                    .getList();

            assertThat(suggestions).extracting(Suggestion::getText)
                                   .contains("minecraft:zombie")
                                   .doesNotContain("zombie", "minecraft:unknown", "unknown");
        }
    }
}
