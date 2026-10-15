package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.suggestion.Suggestion;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Runs the vanilla item_predicate and block_predicate parsers of every node.
class PredicateArgumentTest {
    private static List<String> suggestions(CommandTester tester, String input) {
        return tester.suggestions(input, FakeSender.player("Alice"))
                     .join()
                     .getList()
                     .stream()
                     .map(Suggestion::getText)
                     .collect(Collectors.toList());
    }

    private static BlockInWorld blockInWorld(Block block) {
        LevelReader level = Mockito.mock(LevelReader.class);
        Mockito.when(level.getBlockState(Mockito.any()))
               .thenReturn(block.defaultBlockState());
        return new BlockInWorld(level, BlockPos.ZERO, true);
    }

    @Test
    void item_predicate_matches_the_item() {
        List<String> messages;

        try (CommandTester tester = new CommandTester(() -> new Command("clear") {{
            argument(new ItemPredicateArgument("item")).execute((predicate, ctx) -> {
                ctx.sendMessage(predicate.test(new ItemStack(Items.STONE)) + ","
                        + predicate.test(new ItemStack(Items.DIRT)));
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            tester.execute("clear minecraft:stone", sender);
            tester.execute("clear dirt", sender);
            assertThatThrownBy(() -> tester.execute("clear not_an_item",
                                                    sender)).hasMessageContaining("Unknown item 'minecraft:not_an_item'");
            assertThat(suggestions(tester, "clear diamond_sw")).contains("minecraft:diamond_sword");
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).containsExactly("true,false", "false,true");
    }

    @Test
    void block_predicate_matches_the_block_state() {
        List<String> messages;

        try (CommandTester tester = new CommandTester(() -> new Command("test") {{
            argument(new BlockPredicateArgument("block")).execute((predicate, ctx) -> {
                ctx.sendMessage(predicate.test(blockInWorld(Blocks.STONE)) + ","
                        + predicate.test(blockInWorld(Blocks.DIRT)));
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            tester.execute("test minecraft:stone", sender);
            tester.execute("test dirt", sender);
            assertThatThrownBy(() -> tester.execute("test not_a_block",
                                                    sender)).hasMessageContaining("Unknown block type 'minecraft:not_a_block'");
            assertThat(suggestions(tester, "test oak_sta")).contains("minecraft:oak_stairs");
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).containsExactly("true,false", "false,true");
    }
}
