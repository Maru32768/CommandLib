package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class WorldArgumentTest {
    @Test
    void world_is_resolved_by_dimension_key() {
        FakeSender sender = FakeSender.player("Alice");
        World mockWorld = Mockito.mock(World.class);
        Mockito.when(mockWorld.getName())
               .thenReturn("world");

        try (CommandTester tester = new CommandTester(() -> new Command("tp") {{
            argument(new WorldArgument("world")).execute((world, ctx) -> ctx.sendMessage(world.getName()));
        }}, "test.command").withFakeWorld("minecraft:overworld", mockWorld)) {
            tester.execute("tp minecraft:overworld", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("world");
    }

    @Test
    void unknown_dimension_sends_failure_message() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("tp") {{
            argument(new WorldArgument("world")).execute((world, ctx) -> ctx.sendMessage(world.getName()));
        }}, "test.command")) {
            tester.execute("tp commandlib:missing", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Incorrect argument for command",
                                                                 "tp commandlib:missing<--[HERE]");
        assertThat(sender.getSentMessages()
                         .get(0)
                         .getColor()
                         .getColor()).isEqualTo(new java.awt.Color(0xFF5555));
    }
}
