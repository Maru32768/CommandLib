package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            // A key without a namespace is in minecraft, and the world's name is not its key.
            tester.execute("tp overworld", sender);
            assertThatThrownBy(() -> tester.execute("tp world", sender)).isInstanceOf(RuntimeException.class);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("world", "world");
    }
}
