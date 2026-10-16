package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.enchantments.Enchantment;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("UnstableApiUsage")
class EnchantmentArgumentTest {
    private static CommandTester tester(AtomicReference<Enchantment> received, Enchantment value) {
        return new CommandTester(() -> new Command("enchant") {{
            argument(new EnchantmentArgument("value")).execute((parsed, ctx) -> received.set(parsed));
        }}, "test.command").withFakeRegistryValue(RegistryKey.ENCHANTMENT, "minecraft:sharpness", value);
    }

    @Test
    void value_is_read_from_the_enchantment_registry_by_its_key() {
        AtomicReference<Enchantment> received = new AtomicReference<>();
        Enchantment value = Mockito.mock(Enchantment.class);

        try (CommandTester tester = tester(received, value)) {
            tester.execute("enchant minecraft:sharpness", FakeSender.player("Alice"));
        }

        assertThat(received).hasValue(value);
    }

    @Test
    void unknown_key_is_rejected_while_parsing() {
        AtomicReference<Enchantment> received = new AtomicReference<>();

        try (CommandTester tester = tester(received, Mockito.mock(Enchantment.class))) {
            assertThatThrownBy(() -> tester.execute("enchant commandlib:missing",
                                                    FakeSender.player("Alice"))).hasCauseInstanceOf(CommandSyntaxException.class);
        }

        assertThat(received).hasValue(null);
    }
}
