package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.block.Biome;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("UnstableApiUsage")
class BiomeArgumentTest {
    private static CommandTester tester(AtomicReference<Biome> received, Biome value) {
        return new CommandTester(() -> new Command("biome") {{
            argument(new BiomeArgument("value")).execute((ctx, parsed) -> received.set(parsed));
        }}, "test.command").withFakeRegistryValue(RegistryKey.BIOME, "minecraft:plains", value);
    }

    @Test
    void value_is_read_from_the_biome_registry_by_its_key() {
        AtomicReference<Biome> received = new AtomicReference<>();
        Biome value = Mockito.mock(Biome.class);

        try (CommandTester tester = tester(received, value)) {
            tester.execute("biome minecraft:plains", FakeSender.player("Alice"));
        }

        assertThat(received).hasValue(value);
    }

    @Test
    void unknown_key_is_rejected_while_parsing() {
        AtomicReference<Biome> received = new AtomicReference<>();

        try (CommandTester tester = tester(received, Mockito.mock(Biome.class))) {
            assertThatThrownBy(() -> tester.execute("biome commandlib:missing",
                                                    FakeSender.player("Alice"))).hasCauseInstanceOf(CommandSyntaxException.class);
        }

        assertThat(received).hasValue(null);
    }
}
