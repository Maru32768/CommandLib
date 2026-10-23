package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The component is last, since up to 1.20.2 vanilla's reader also consumes the character after it.
class ComponentArgumentTest {
    @Test
    void json_text_is_parsed_into_a_component() {
        List<BaseComponent> components = new ArrayList<>();
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("tell") {{
            argument(new IntegerArgument("count"), new ComponentArgument("text")).execute((ctx, count, text) -> {
                components.add(text);
                ctx.sendMessage(text);
                ctx.sendMessage("count=" + count);
            });
        }}, "test.command")) {
            tester.execute("tell 2 {\"text\":\"Hello {world}\",\"color\":\"red\"}", sender);
            tester.execute("tell 3 \"plain\"", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Hello {world}", "count=2", "plain", "count=3");
        assertThat(components.get(0)
                             .getColor()).isEqualTo(ChatColor.RED);
    }

    @Test
    void malformed_json_is_a_syntax_error() {
        try (CommandTester tester = new CommandTester(ComponentArgumentTest::command, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            assertThatThrownBy(() -> tester.execute("tell {\"text\":",
                                                    sender)).hasCauseInstanceOf(CommandSyntaxException.class);
            assertThatThrownBy(() -> tester.execute("tell {\"text\":}",
                                                    sender)).hasCauseInstanceOf(CommandSyntaxException.class);
        }
    }

    @Test
    void there_are_no_suggestions() {
        try (CommandTester tester = new CommandTester(ComponentArgumentTest::command, "test.command")) {
            assertThat(tester.suggest("tell {", FakeSender.player("Alice"))).isEmpty();
        }
    }

    private static Command command() {
        return new Command("tell") {{
            argument(new ComponentArgument("text")).execute((ctx, text) -> ctx.sendMessage(text));
        }};
    }
}
