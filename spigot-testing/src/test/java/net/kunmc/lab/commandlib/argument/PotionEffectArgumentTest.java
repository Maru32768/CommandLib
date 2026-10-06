package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PotionEffectArgumentTest {
    @Test
    void effect_id_resolves_to_one_tick_potion_effect() {
        FakeSender sender = FakeSender.player("Alice");
        PotionEffectType speed = speedType();

        try (MockedStatic<PotionEffectType> potionStatic = Mockito.mockStatic(PotionEffectType.class);
             CommandTester tester = new CommandTester(PotionEffectArgumentTest::effectCommand, "test.command")) {
            potionStatic.when(() -> PotionEffectType.getByName("SPEED"))
                        .thenReturn(speed);
            tester.execute("effect minecraft:speed", sender);
            tester.execute("effect speed", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("SPEED duration=1 amplifier=0",
                                                                 "SPEED duration=1 amplifier=0");
    }

    @Test
    void unknown_effect_is_a_syntax_error() {
        try (MockedStatic<PotionEffectType> ignored = Mockito.mockStatic(PotionEffectType.class);
             CommandTester tester = new CommandTester(PotionEffectArgumentTest::effectCommand, "test.command")) {
            assertThatThrownBy(() -> tester.execute("effect minecraft:not_an_effect",
                                                    FakeSender.player("Alice"))).hasCauseInstanceOf(
                    CommandSyntaxException.class);
        }
    }

    private static PotionEffectType speedType() {
        PotionEffectType type = Mockito.mock(PotionEffectType.class);
        Mockito.when(type.getName())
               .thenReturn("SPEED");
        Mockito.when(type.createEffect(1, 0))
               .thenReturn(new PotionEffect(type, 1, 0));
        return type;
    }

    private static Command effectCommand() {
        return new Command("effect") {{
            argument(new PotionEffectArgument("type")).execute((effect, ctx) -> {
                ctx.sendMessage(effect.getType()
                                      .getName() + " duration=" + effect.getDuration() + " amplifier=" + effect.getAmplifier());
            });
        }};
    }
}
