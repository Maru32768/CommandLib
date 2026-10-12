package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;

@SuppressWarnings("UnstableApiUsage")
public class RecipeArgument extends Argument<Recipe, RecipeArgument> {
    public RecipeArgument(String name) {
        super(name, ArgumentTypes.namespacedKey());
        addSuggestionAction(sb -> Bukkit.recipeIterator()
                                        .forEachRemaining(recipe -> {
                                            if (recipe instanceof Keyed) {
                                                NamespacedKeyArgumentSupport.suggestKey(sb, ((Keyed) recipe).getKey());
                                            }
                                        }));
    }

    @Override
    public Recipe cast(Object parsedArgument) {
        return (Recipe) parsedArgument;
    }

    @Override
    protected Recipe parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        NamespacedKey key = ctx.getHandle()
                               .getArgument(name(), NamespacedKey.class);
        Recipe recipe = Bukkit.getRecipe(key);
        if (recipe == null) {
            throw new ArgumentParseException(x -> x.sendFailure(key + " is not a known recipe"));
        }
        return recipe;
    }
}
