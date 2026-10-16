package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeArgumentTest {
    private static Command command(AtomicReference<Recipe> received) {
        return new Command("recipe") {{
            argument(new RecipeArgument("value")).execute((value, ctx) -> received.set(value));
        }};
    }

    @Test
    void recipe_is_resolved_by_namespaced_key() {
        AtomicReference<Recipe> received = new AtomicReference<>();
        Recipe recipe = Mockito.mock(Recipe.class);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(() -> command(received), "test.command")) {
            bukkit.when(() -> Bukkit.getRecipe(NamespacedKey.minecraft("stick")))
                  .thenReturn(recipe);
            tester.execute("recipe stick", FakeSender.player("Alice"));
        }

        assertThat(received).hasValue(recipe);
    }

    @Test
    void unknown_recipe_sends_vanilla_not_found_message() {
        FakeSender sender = FakeSender.player("Alice");
        AtomicReference<Recipe> received = new AtomicReference<>();

        try (MockedStatic<Bukkit> ignored = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(() -> command(received), "test.command")) {
            tester.execute("recipe commandlib:missing", sender);
        }

        assertThat(received).hasValue(null);
        assertThat(sender.getSentMessages()).containsExactly(Component.translatable("recipe.notFound",
                                                                                    Component.text("commandlib:missing"))
                                                                      .color(NamedTextColor.RED));
    }

    @Test
    void suggestions_are_keys_of_keyed_recipes_containing_the_input() {
        Recipe stick = Mockito.mock(Recipe.class,
                                    Mockito.withSettings()
                                           .extraInterfaces(Keyed.class));
        Mockito.when(((Keyed) stick).getKey())
               .thenReturn(NamespacedKey.minecraft("stick"));
        Recipe torch = Mockito.mock(Recipe.class,
                                    Mockito.withSettings()
                                           .extraInterfaces(Keyed.class));
        Mockito.when(((Keyed) torch).getKey())
               .thenReturn(NamespacedKey.minecraft("torch"));
        Recipe unkeyed = Mockito.mock(Recipe.class);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(() -> command(new AtomicReference<>()), "test.command")) {
            bukkit.when(Bukkit::recipeIterator)
                  .thenAnswer(x -> List.of(stick, unkeyed, torch)
                                       .iterator());

            assertThat(tester.suggest("recipe STI", FakeSender.player("Alice"))).containsExactly("minecraft:stick");
            assertThat(tester.suggest("recipe ", FakeSender.player("Alice"))).contains("minecraft:stick",
                                                                                       "minecraft:torch");
        }
    }
}
