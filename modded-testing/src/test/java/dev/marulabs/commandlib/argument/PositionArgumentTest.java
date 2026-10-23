package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Runs the vanilla vec3 and block_pos parsers of every node.
class PositionArgumentTest {
    @Test
    void integer_coordinates_are_centered_unless_disabled() {
        List<String> messages;

        try (CommandTester tester = new CommandTester(() -> new Command("pos") {{
            argument(new LocationArgument("centered"),
                     new LocationArgument("exact", false)).execute((ctx, centered, exact) -> {
                         ctx.sendMessage(centered.getX() + " " + centered.getZ() + " " + exact.getX() + " "
                                 + exact.getZ());
                     });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            tester.execute("pos 1 64 1 1 64 1", sender);
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).containsExactly("1.5 1.5 1.0 1.0");
    }

    @Test
    void block_position_reads_integers_only() {
        List<String> messages;

        try (CommandTester tester = new CommandTester(() -> new Command("pos") {{
            argument(new BlockPositionArgument("pos")).execute((ctx, pos) -> ctx.sendMessage(pos.getX() + ","
                    + pos.getY() + "," + pos.getZ()));
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            tester.execute("pos 1 64 -3", sender);
            assertThatThrownBy(() -> tester.execute("pos 1.5 64 3",
                                                    sender)).hasMessageContaining("Invalid integer '1.5'");
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).containsExactly("1,64,-3");
    }
}
