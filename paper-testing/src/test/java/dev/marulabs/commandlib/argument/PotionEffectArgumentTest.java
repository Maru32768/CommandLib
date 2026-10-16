package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("UnstableApiUsage")
class PotionEffectArgumentTest {
    private static CommandTester tester(AtomicReference<PotionEffect> received, PotionEffectType speed) {
        return new CommandTester(() -> new Command("effect") {{
            argument(new PotionEffectArgument("value")).execute((effect, ctx) -> received.set(effect));
        }}, "test.command").withFakeRegistryValue(RegistryKey.MOB_EFFECT, "minecraft:speed", speed);
    }

    @Test
    void effect_key_resolves_to_a_one_tick_level_one_effect_of_that_type() {
        AtomicReference<PotionEffect> received = new AtomicReference<>();
        PotionEffectType speed = Mockito.mock(PotionEffectType.class);
        PotionEffect oneTick = Mockito.mock(PotionEffect.class);
        Mockito.when(speed.createEffect(1, 0))
               .thenReturn(oneTick);

        try (CommandTester tester = tester(received, speed)) {
            tester.execute("effect minecraft:speed", FakeSender.player("Alice"));
        }

        assertThat(received).hasValue(oneTick);
    }

    @Test
    void unknown_key_is_rejected_while_parsing() {
        AtomicReference<PotionEffect> received = new AtomicReference<>();

        try (CommandTester tester = tester(received, Mockito.mock(PotionEffectType.class))) {
            assertThatThrownBy(() -> tester.execute("effect commandlib:missing",
                                                    FakeSender.player("Alice"))).hasCauseInstanceOf(CommandSyntaxException.class);
        }

        assertThat(received).hasValue(null);
    }
}
