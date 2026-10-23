package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BlockPositionArgumentTest {
    private static Command positionCommand() {
        return new Command("pos") {{
            argument(new BlockPositionArgument("pos")).execute((ctx, pos) -> {
                ctx.sendMessage(pos.getX() + "," + pos.getY() + "," + pos.getZ());
            });
        }};
    }

    @Test
    void integers_are_a_block_location() {
        FakeSender sender = FakeSender.player("Steve");

        try (CommandTester tester = new CommandTester(BlockPositionArgumentTest::positionCommand, "test.command")) {
            tester.execute("pos 1 64 -3", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("1.0,64.0,-3.0");
    }

    @Test
    void decimal_position_is_a_syntax_error() {
        try (CommandTester tester = new CommandTester(BlockPositionArgumentTest::positionCommand, "test.command")) {
            assertThatThrownBy(() -> tester.execute("pos 1.5 64 3",
                                                    FakeSender.player("Steve"))).hasCauseInstanceOf(CommandSyntaxException.class);
        }
    }
}
