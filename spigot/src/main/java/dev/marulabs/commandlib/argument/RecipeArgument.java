package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentNamespacedKey;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;

public class RecipeArgument extends Argument<Recipe, RecipeArgument> {
    public RecipeArgument(String name) {
        super(name,
              NMSArgumentNamespacedKey.create()
                                      .argument());
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
        String value = NMSArgumentNamespacedKey.create()
                                               .parse(ctx.getHandle(), name());
        NamespacedKey key = NamespacedKeyArgumentSupport.parseKey(value);
        Recipe recipe = Bukkit.getRecipe(key);
        if (recipe == null) {
            throw new ArgumentParseException(x -> x.sendFailure(value + " is not a known recipe"));
        }
        return recipe;
    }
}
