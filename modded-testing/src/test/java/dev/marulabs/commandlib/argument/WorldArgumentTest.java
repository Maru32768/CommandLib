package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorldArgumentTest {
    @Test
    void level_of_the_server_is_read_by_its_dimension_key_and_suggested() {
        List<String> messages;

        try (CommandTester tester = new CommandTester(() -> new Command("world") {{
            argument(new WorldArgument("world")).execute((ctx, world) -> {
                ctx.sendMessage(world.dimension()
                                     .location()
                                     .toString());
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            ServerLevel overworld = Mockito.mock(ServerLevel.class);
            Mockito.when(overworld.dimension())
                   .thenReturn(Level.OVERWORLD);
            tester.withLevel(overworld);

            tester.execute("world minecraft:overworld", sender);
            tester.execute("world minecraft:the_nether", sender);
            assertThat(tester.suggest("world minecraft:o", sender)).containsExactly("minecraft:overworld");
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).hasSize(2)
                            .startsWith("minecraft:overworld");
        assertThat(messages.get(1)).contains("minecraft:the_nether");
    }
}
