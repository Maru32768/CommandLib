package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.CommandTester;
import dev.marulabs.cadence.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NamespacedKeyArgumentTest {
    @Test
    void namespaced_key_is_parsed_with_paper_argument_type() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("key") {{
            argument(new NamespacedKeyArgument("value")).execute((ctx, key) -> ctx.sendMessage(key.toString()));
        }}, "test.command")) {
            tester.execute("key cadence:example/path", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("cadence:example/path");
    }
}
