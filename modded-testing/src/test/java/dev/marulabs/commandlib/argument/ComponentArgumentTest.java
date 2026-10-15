package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Runs the vanilla component parser of every node. The component is last, since up to 1.20.2 vanilla's reader also
// consumes the character after it.
class ComponentArgumentTest {
    @Test
    void json_text_is_parsed_into_a_component() {
        List<Component> components = new ArrayList<>();
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("tell") {{
            argument(new IntegerArgument("count"), new ComponentArgument("text")).execute((count, text, ctx) -> {
                components.add(text);
                ctx.sendMessage(text);
                ctx.sendMessage("count=" + count);
            });
        }}, "test.command")) {
            tester.execute("tell 2 {\"text\":\"Hello {world}\",\"color\":\"red\"}", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Hello {world}", "count=2");
        assertThat(Objects.requireNonNull(components.get(0)
                                                    .getStyle()
                                                    .getColor())
                          .serialize()).isEqualTo("red");
    }

    @Test
    void malformed_json_is_a_syntax_error() {
        try (CommandTester tester = new CommandTester(ComponentArgumentTest::command, "test.command")) {
            assertThatThrownBy(() -> tester.execute("tell {\"text\":",
                                                    FakeSender.player("Alice"))).hasMessageContaining("Command syntax error");
        }
    }

    @Test
    void there_are_no_suggestions() {
        try (CommandTester tester = new CommandTester(ComponentArgumentTest::command, "test.command")) {
            assertThat(tester.suggestions("tell {", FakeSender.player("Alice"))
                             .join()
                             .getList()).isEmpty();
        }
    }

    private static Command command() {
        return new Command("tell") {{
            argument(new ComponentArgument("text")).execute((text, ctx) -> ctx.sendMessage(text));
        }};
    }
}
