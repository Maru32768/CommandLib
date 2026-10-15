package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BlockPredicateArgumentTest {
    private static Command testCommand() {
        return new Command("test") {{
            argument(new BlockPredicateArgument("block")).execute((predicate, ctx) -> {
                ctx.sendMessage(predicate.test(block(Material.STONE)) + "," + predicate.test(block(Material.DIRT)));
            });
        }};
    }

    private static Block block(Material material) {
        Block block = Mockito.mock(Block.class);
        Mockito.when(block.getType())
               .thenReturn(material);
        return block;
    }

    @Test
    void block_id_matches_blocks_of_that_type() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(BlockPredicateArgumentTest::testCommand, "test.command")) {
            tester.execute("test stone", sender);
            tester.execute("test minecraft:dirt", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("true,false", "false,true");
    }

    @Test
    void item_without_block_form_is_a_syntax_error() {
        try (CommandTester tester = new CommandTester(BlockPredicateArgumentTest::testCommand, "test.command")) {
            assertThatThrownBy(() -> tester.execute("test diamond_sword",
                                                    FakeSender.player("Alice"))).hasMessageContaining("Unknown block 'minecraft:diamond_sword'");
        }
    }

    @Test
    void suggestions_are_block_ids() {
        try (CommandTester tester = new CommandTester(BlockPredicateArgumentTest::testCommand, "test.command")) {
            assertThat(tester.suggest("test minecraft:oak_sta",
                                      FakeSender.player("Alice"))).containsExactly("minecraft:oak_stairs");
        }
    }
}
