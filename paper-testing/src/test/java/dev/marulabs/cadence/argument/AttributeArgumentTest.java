package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.CommandTester;
import dev.marulabs.cadence.FakeSender;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.attribute.Attribute;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("UnstableApiUsage")
class AttributeArgumentTest {
    private static CommandTester tester(AtomicReference<Attribute> received, Attribute value) {
        return new CommandTester(() -> new Command("attribute") {{
            argument(new AttributeArgument("value")).execute((ctx, parsed) -> received.set(parsed));
        }}, "test.command").withFakeRegistryValue(RegistryKey.ATTRIBUTE, "minecraft:max_health", value);
    }

    @Test
    void value_is_read_from_the_attribute_registry_by_its_key() {
        AtomicReference<Attribute> received = new AtomicReference<>();
        Attribute value = Mockito.mock(Attribute.class);

        try (CommandTester tester = tester(received, value)) {
            tester.execute("attribute minecraft:max_health", FakeSender.player("Alice"));
        }

        assertThat(received).hasValue(value);
    }

    @Test
    void unknown_key_is_rejected_while_parsing() {
        AtomicReference<Attribute> received = new AtomicReference<>();

        try (CommandTester tester = tester(received, Mockito.mock(Attribute.class))) {
            assertThatThrownBy(() -> tester.execute("attribute cadence:missing",
                                                    FakeSender.player("Alice"))).hasCauseInstanceOf(CommandSyntaxException.class);
        }

        assertThat(received).hasValue(null);
    }
}
