package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.CommandTester;
import dev.marulabs.cadence.FakeSender;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Sound;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("UnstableApiUsage")
class SoundArgumentTest {
    private static CommandTester tester(AtomicReference<Sound> received, Sound value) {
        return new CommandTester(() -> new Command("playsound") {{
            argument(new SoundArgument("value")).execute((ctx, parsed) -> received.set(parsed));
        }}, "test.command").withFakeRegistryValue(RegistryKey.SOUND_EVENT, "minecraft:block.anvil.land", value);
    }

    @Test
    void value_is_read_from_the_sound_event_registry_by_its_key() {
        AtomicReference<Sound> received = new AtomicReference<>();
        Sound value = Mockito.mock(Sound.class);

        try (CommandTester tester = tester(received, value)) {
            tester.execute("playsound minecraft:block.anvil.land", FakeSender.player("Alice"));
        }

        assertThat(received).hasValue(value);
    }

    @Test
    void unknown_key_is_rejected_while_parsing() {
        AtomicReference<Sound> received = new AtomicReference<>();

        try (CommandTester tester = tester(received, Mockito.mock(Sound.class))) {
            assertThatThrownBy(() -> tester.execute("playsound cadence:missing",
                                                    FakeSender.player("Alice"))).hasCauseInstanceOf(CommandSyntaxException.class);
        }

        assertThat(received).hasValue(null);
    }
}
