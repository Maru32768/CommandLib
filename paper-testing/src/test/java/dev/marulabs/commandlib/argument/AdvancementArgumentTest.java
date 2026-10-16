package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AdvancementArgumentTest {
    private static Command command(AtomicReference<Advancement> received) {
        return new Command("advancement") {{
            argument(new AdvancementArgument("value")).execute((value, ctx) -> received.set(value));
        }};
    }

    @Test
    void advancement_is_resolved_by_namespaced_key() {
        AtomicReference<Advancement> received = new AtomicReference<>();
        Advancement advancement = Mockito.mock(Advancement.class);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(() -> command(received), "test.command")) {
            bukkit.when(() -> Bukkit.getAdvancement(NamespacedKey.minecraft("story/mine_stone")))
                  .thenReturn(advancement);
            tester.execute("advancement story/mine_stone", FakeSender.player("Alice"));
        }

        assertThat(received).hasValue(advancement);
    }

    @Test
    void unknown_advancement_sends_vanilla_not_found_message() {
        FakeSender sender = FakeSender.player("Alice");
        AtomicReference<Advancement> received = new AtomicReference<>();

        try (MockedStatic<Bukkit> ignored = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(() -> command(received), "test.command")) {
            tester.execute("advancement story/missing", sender);
        }

        assertThat(received).hasValue(null);
        assertThat(sender.getSentMessages()).containsExactly(Component.translatable("advancement.advancementNotFound",
                                                                                    Component.text("minecraft:story/missing"))
                                                                      .color(NamedTextColor.RED));
    }

    @Test
    void suggestions_are_advancement_keys_containing_the_input() {
        Advancement mineStone = keyed(NamespacedKey.minecraft("story/mine_stone"));
        Advancement root = keyed(NamespacedKey.minecraft("adventure/root"));

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(() -> command(new AtomicReference<>()), "test.command")) {
            bukkit.when(Bukkit::advancementIterator)
                  .thenAnswer(x -> List.of(mineStone, root)
                                       .iterator());

            assertThat(tester.suggest("advancement STONE", FakeSender.player("Alice"))).containsExactly(
                    "minecraft:story/mine_stone");
            assertThat(tester.suggest("advancement ", FakeSender.player("Alice"))).contains(
                    "minecraft:story/mine_stone",
                    "minecraft:adventure/root");
        }
    }

    private static Advancement keyed(NamespacedKey key) {
        Advancement advancement = Mockito.mock(Advancement.class);
        Mockito.when(advancement.getKey())
               .thenReturn(key);
        return advancement;
    }
}
