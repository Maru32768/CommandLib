package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringArgumentTest {
    @Test
    void single_word_is_parsed() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("say") {{
            argument(new StringArgument("message")).execute((ctx, message) -> {
                ctx.sendMessage(">" + message);
            });
        }}, "test.command")) {
            tester.execute("say hello", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly(">hello");
    }

    @Test
    void quoted_phrase_is_parsed() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("say") {{
            argument(new StringArgument("message")).execute((ctx, message) -> {
                ctx.sendMessage(">" + message);
            });
        }}, "test.command")) {
            tester.execute("say \"hello world\"", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly(">hello world");
    }
}
