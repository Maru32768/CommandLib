package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BlockPositionArgumentTest {
    private static Command positionCommand() {
        return new Command("pos") {{
            argument(new BlockPositionArgument("pos")).execute((pos, ctx) -> {
                ctx.sendMessage(pos.getX() + "," + pos.getY() + "," + pos.getZ() + ","
                        + (pos.getWorld() != null ? pos.getWorld()
                                                       .getName()
                                : "none"));
            });
        }};
    }

    @Test
    void integers_are_a_block_location_in_the_sender_world() {
        FakeSender sender = FakeSender.player("Steve");
        World world = Mockito.mock(World.class);
        Mockito.when(world.getName())
               .thenReturn("world");
        Mockito.when(((Player) sender.asSender()).getWorld())
               .thenReturn(world);

        try (CommandTester tester = new CommandTester(BlockPositionArgumentTest::positionCommand, "test.command")) {
            tester.execute("pos 1 64 -3", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("1.0,64.0,-3.0,world");
    }

    @Test
    void decimal_and_incomplete_positions_are_syntax_errors() {
        try (CommandTester tester = new CommandTester(BlockPositionArgumentTest::positionCommand, "test.command")) {
            assertThatThrownBy(() -> tester.execute("pos 1.5 64 3",
                                                    FakeSender.player("Steve"))).hasCauseInstanceOf(CommandSyntaxException.class);
            assertThatThrownBy(() -> tester.execute("pos 1 64",
                                                    FakeSender.player("Steve"))).hasCauseInstanceOf(CommandSyntaxException.class);
        }
    }
}
