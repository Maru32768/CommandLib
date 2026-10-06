package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnumArgumentTest {
    enum Direction {
        NORTH,
        SOUTH,
        EAST,
        WEST
    }

    @Test
    void enum_constant_is_resolved_case_insensitively() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("go") {{
            argument(new EnumArgument<>("dir", Direction.class)).execute((dir, ctx) -> {
                ctx.sendMessage("going " + dir.name());
            });
        }}, "test.command")) {
            tester.execute("go north", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("going NORTH");
    }

    @Test
    void unknown_enum_constant_sends_failure_message() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(new Command("go") {{
            argument(new EnumArgument<>("dir", Direction.class)).execute((dir, ctx) -> {
                ctx.sendMessage("going " + dir.name());
            });
        }}, "test.command")) {
            tester.execute("go up", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Incorrect argument for command",
                                                                 "go up<--[HERE]");
        assertThat(sender.getSentMessages()
                         .get(0)
                         .getColor()
                         .getColor()).isEqualTo(new java.awt.Color(0xFF5555));
    }
}
