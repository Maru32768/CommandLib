package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NamespacedKeyArgumentTest {
    @Test
    void namespaced_key_is_parsed_with_paper_argument_type() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("key") {{
            argument(new NamespacedKeyArgument("value")).execute((ctx, key) -> ctx.sendMessage(key.toString()));
        }}, "test.command")) {
            tester.execute("key commandlib:example/path", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("commandlib:example/path");
    }
}
