package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class BlockDataArgumentTest {
    @Test
    void material_name_resolves_to_block_data() {
        FakeSender sender = FakeSender.player("Alice");
        BlockData mockBlockData = Mockito.mock(BlockData.class);
        Mockito.when(mockBlockData.getMaterial())
               .thenReturn(Material.STONE);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(() -> new Command("setblock") {{
                 argument(new BlockDataArgument("block")).execute((block, ctx) -> {
                     ctx.sendMessage(block.getMaterial()
                                          .name());
                 });
             }}, "test.command")) {
            bukkit.when(() -> Bukkit.createBlockData(Material.STONE))
                  .thenReturn(mockBlockData);
            tester.execute("setblock stone", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("STONE");
    }
}
