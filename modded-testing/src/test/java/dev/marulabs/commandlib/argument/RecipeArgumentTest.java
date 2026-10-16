package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

//? if >=1.20.2
/*import net.minecraft.world.item.crafting.RecipeHolder;*/
//? if >=1.21.2 {
/*import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
*///?}

import static org.assertj.core.api.Assertions.assertThat;

class RecipeArgumentTest {
    @Test
    void recipe_of_the_server_is_read_by_its_key_and_suggested() {
        List<String> messages;
        List<String> suggestions;
        ResourceLocation id = ResourceLocation.tryParse("minecraft:oak_planks");

        try (CommandTester tester = new CommandTester(() -> new Command("recipe") {{
            argument(new RecipeArgument("recipe")).execute((recipe, ctx) -> {
                //? if >=1.21.2 {
                /*ctx.sendMessage(recipe.id()
                  .location()
                  .toString());
                *///?} elif >=1.20.2 {
                /*ctx.sendMessage(recipe.id()
                  .toString());
                *///?} else
                ctx.sendMessage(String.valueOf(recipe.getId()));
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            Recipe<?> recipe = Mockito.mock(Recipe.class);
            RecipeManager recipes = Mockito.mock(RecipeManager.class);
            //? if >=1.21.2 {
            /*ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
            RecipeHolder<?> holder = new RecipeHolder<>(key, recipe);
            Mockito.doReturn(Optional.of(holder))
                   .when(recipes)
                   .byKey(key);
            Mockito.doReturn(List.of(holder))
                   .when(recipes)
                   .getRecipes();
            *///?} elif >=1.20.2 {
            /*Mockito.doReturn(Optional.of(new RecipeHolder<>(id, recipe)))
                   .when(recipes)
                   .byKey(id);
            Mockito.when(recipes.getRecipeIds())
                   .thenAnswer(invocation -> Stream.of(id));
            *///?} else {
            Mockito.when(recipe.getId())
                   .thenReturn(id);
            Mockito.doReturn(Optional.of(recipe))
                   .when(recipes)
                   .byKey(id);
            Mockito.when(recipes.getRecipeIds())
                   .thenAnswer(invocation -> Stream.of(id));
            //?}
            tester.withServer(server -> Mockito.when(server.getRecipeManager())
                                               .thenReturn(recipes));

            tester.execute("recipe minecraft:oak_planks", sender);
            tester.execute("recipe commandlib:missing", sender);
            suggestions = tester.suggest("recipe oak_pl", sender);
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).hasSize(2)
                            .startsWith("minecraft:oak_planks");
        assertThat(messages.get(1)).contains("commandlib:missing");
        assertThat(suggestions).containsExactly("minecraft:oak_planks");
    }
}
