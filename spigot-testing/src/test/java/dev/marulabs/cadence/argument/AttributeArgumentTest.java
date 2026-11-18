package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.CommandTester;
import dev.marulabs.cadence.FakeSender;
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
