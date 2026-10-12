package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityTypeArgumentTest {
    @Test
    void entity_type_is_parsed_case_insensitively() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("entitytype") {{
            argument(new EntityTypeArgument("value")).execute((type, ctx) -> ctx.sendMessage(type.name()));
        }}, "test.command")) {
            tester.execute("entitytype zombie", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("ZOMBIE");
    }
}
