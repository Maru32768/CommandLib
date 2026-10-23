package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemPredicateArgumentTest {
    private static Command clearCommand() {
        return new Command("clear") {{
            argument(new ItemPredicateArgument("item")).execute((ctx, predicate) -> {
                ctx.sendMessage(predicate.test(item(Material.STONE)) + "," + predicate.test(item(Material.DIRT)));
            });
        }};
    }

    private static ItemStack item(Material material) {
        ItemStack item = Mockito.mock(ItemStack.class);
        Mockito.when(item.getType())
               .thenReturn(material);
        return item;
    }

    @Test
    void item_id_matches_items_of_that_type() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(ItemPredicateArgumentTest::clearCommand, "test.command")) {
            tester.execute("clear stone", sender);
            tester.execute("clear minecraft:dirt", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("true,false", "false,true");
    }

    @Test
    void unknown_item_is_a_syntax_error() {
        try (CommandTester tester = new CommandTester(ItemPredicateArgumentTest::clearCommand, "test.command")) {
            assertThatThrownBy(() -> tester.execute("clear not_an_item",
                                                    FakeSender.player("Alice"))).hasMessageContaining("Unknown item 'not_an_item'");
        }
    }

    @Test
    void suggestions_are_item_ids() {
        try (CommandTester tester = new CommandTester(ItemPredicateArgumentTest::clearCommand, "test.command")) {
            assertThat(tester.suggest("clear diamond_sw",
                                      FakeSender.player("Alice"))).containsExactly("minecraft:diamond_sword");
        }
    }
}
