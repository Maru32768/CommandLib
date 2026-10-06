package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemStackArgumentTest {
    @Test
    void material_name_resolves_to_item_stack() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(ItemStackArgumentTest::giveCommand, "test.command")) {
            tester.execute("give stone", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("STONE x1");
    }

    @Test
    void namespaced_item_id_resolves_to_item_stack() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(ItemStackArgumentTest::giveCommand, "test.command")) {
            tester.execute("give minecraft:diamond_sword", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("DIAMOND_SWORD x1");
    }

    @Test
    void unknown_item_is_a_syntax_error() {
        try (CommandTester tester = new CommandTester(ItemStackArgumentTest::giveCommand, "test.command")) {
            assertThatThrownBy(() -> tester.execute("give not_a_real_material",
                                                    FakeSender.player("Alice"))).hasCauseInstanceOf(
                    CommandSyntaxException.class);
        }
    }

    @Test
    void block_without_item_form_is_a_syntax_error() {
        try (CommandTester tester = new CommandTester(ItemStackArgumentTest::giveCommand, "test.command")) {
            assertThatThrownBy(() -> tester.execute("give minecraft:water",
                                                    FakeSender.player("Alice"))).hasCauseInstanceOf(
                    CommandSyntaxException.class);
        }
    }

    @Test
    void uppercase_item_id_is_a_syntax_error() {
        try (CommandTester tester = new CommandTester(ItemStackArgumentTest::giveCommand, "test.command")) {
            assertThatThrownBy(() -> tester.execute("give STONE", FakeSender.player("Alice"))).hasCauseInstanceOf(
                    CommandSyntaxException.class);
        }
    }

    private static Command giveCommand() {
        return new Command("give") {{
            argument(new ItemStackArgument("item")).execute((item, ctx) -> {
                ctx.sendMessage(item.getType()
                                    .name() + " x" + item.getAmount());
            });
        }};
    }
}
