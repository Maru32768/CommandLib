package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class EnchantmentArgumentTest {
    @Test
    void enchantment_name_resolves_to_enchantment() {
        FakeSender sender = FakeSender.player("Alice");
        Enchantment mockEnchantment = Mockito.mock(Enchantment.class);
        Mockito.when(mockEnchantment.getKey())
               .thenReturn(NamespacedKey.minecraft("sharpness"));

        try (MockedStatic<Enchantment> enchStatic = Mockito.mockStatic(Enchantment.class);
             CommandTester tester = new CommandTester(() -> new Command("enchant") {{
                 argument(new EnchantmentArgument("type")).execute((ctx, ench) -> {
                     ctx.sendMessage(ench.getKey()
                                         .getKey());
                 });
             }}, "test.command")) {
            enchStatic.when(() -> Enchantment.getByKey(NamespacedKey.minecraft("sharpness")))
                      .thenReturn(mockEnchantment);
            tester.execute("enchant sharpness", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("sharpness");
    }
}
