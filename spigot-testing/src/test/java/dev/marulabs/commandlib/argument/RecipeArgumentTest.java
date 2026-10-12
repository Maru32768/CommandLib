package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeArgumentTest {
    @Test
    void recipe_is_resolved_by_namespaced_key() {
        FakeSender sender = FakeSender.player("Alice");
        NamespacedKey key = NamespacedKey.minecraft("stick");
        Recipe recipe = Mockito.mock(Recipe.class);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getRecipe(key))
                  .thenReturn(recipe);
            try (CommandTester tester = new CommandTester(() -> new Command("recipe") {{
                argument(new RecipeArgument("value")).execute((value, ctx) -> ctx.sendMessage("ok"));
            }}, "test.command")) {
                tester.execute("recipe stick", sender);
            }
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("ok");
    }

    @Test
    void suggestions_include_recipe_keys() throws Exception {
        NamespacedKey key = NamespacedKey.minecraft("stick");
        Recipe recipe = Mockito.mock(Recipe.class,
                                     Mockito.withSettings()
                                            .extraInterfaces(org.bukkit.Keyed.class));
        Mockito.when(((org.bukkit.Keyed) recipe).getKey())
               .thenReturn(key);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::recipeIterator)
                  .thenReturn(java.util.List.of(recipe)
                                            .iterator());
            try (CommandTester tester = new CommandTester(() -> new Command("recipe") {{
                argument(new RecipeArgument("value")).execute((value, ctx) -> {
                });
            }}, "test.command")) {
                var suggestions = tester.suggestions("recipe sti", FakeSender.player("Alice"))
                                        .get()
                                        .getList();

                assertThat(suggestions).extracting(com.mojang.brigadier.suggestion.Suggestion::getText)
                                       .contains("minecraft:stick");
            }
        }
    }
}
