package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComponentArgumentTest {
    @Test
    void json_text_is_parsed_into_a_component() {
        List<Component> components = new ArrayList<>();
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("tell") {{
            argument(new IntegerArgument("count"), new ComponentArgument("text")).execute((ctx, count, text) -> {
                components.add(text);
                ctx.sendMessage(text);
                ctx.sendMessage("count=" + count);
            });
        }}, "test.command")) {
            tester.execute("tell 2 {\"text\":\"Hello {world}\",\"color\":\"red\"}", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Hello {world}", "count=2");
        assertThat(components.get(0)
                             .color()).isEqualTo(NamedTextColor.RED);
    }

    @Test
    void malformed_json_is_a_syntax_error() {
        try (CommandTester tester = new CommandTester(ComponentArgumentTest::command, "test.command")) {
            assertThatThrownBy(() -> tester.execute("tell {\"text\":",
                                                    FakeSender.player("Alice"))).hasCauseInstanceOf(CommandSyntaxException.class);
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
