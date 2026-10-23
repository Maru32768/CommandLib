package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AttributeArgumentTest {
    @Test
    void attribute_is_resolved_by_registry_key() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("attribute") {{
            argument(new AttributeArgument("value")).execute((ctx, attribute) -> ctx.sendMessage(attribute.getKey()
                                                                                                          .toString()));
        }}, "test.command")) {
            tester.execute("attribute generic.max_health", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("minecraft:generic.max_health");
    }
}
