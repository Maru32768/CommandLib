package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AttributeArgumentTest {
    @Test
    void attribute_is_parsed_case_insensitively() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("attribute") {{
            argument(new AttributeArgument("value")).execute((attribute, ctx) -> ctx.sendMessage(attribute.name()));
        }}, "test.command")) {
            tester.execute("attribute generic_max_health", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("GENERIC_MAX_HEALTH");
    }
}
