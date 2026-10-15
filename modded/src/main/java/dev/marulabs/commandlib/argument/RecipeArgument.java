package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import net.minecraft.commands.arguments.ResourceLocationArgument;
//? if >=1.20.2 {
/*import net.minecraft.world.item.crafting.RecipeHolder;
*///?} else
import net.minecraft.world.item.crafting.Recipe;

// A recipe of the server by its key, such as minecraft:oak_planks; the loaded recipes are suggested, and an unknown
// key fails with vanilla's error when the command runs. The value is vanilla's: a RecipeHolder from 1.20.2, which
// ServerPlayer#awardRecipes takes there, and the Recipe before.
//? if >=1.20.2 {
/*public class RecipeArgument extends Argument<RecipeHolder<?>, RecipeArgument> {
*///?} else
public class RecipeArgument extends Argument<Recipe<?>, RecipeArgument> {
    public RecipeArgument(String name) {
        super(name, ResourceLocationArgument.id());
        addSuggestionProvider(sb -> RegistryEntries.suggest(sb,
                                                            sb.getContext()
                                                              .getHandle()
                                                              .getSource()
                                                              .getRecipeNames()));
    }

    //? if >=1.20.2 {
    /*@Override
    public RecipeHolder<?> cast(Object parsedArgument) {
        return (RecipeHolder<?>) parsedArgument;
    }

    @Override
    protected RecipeHolder<?> parseImpl(CommandContext ctx) throws CommandSyntaxException {
        return ResourceLocationArgument.getRecipe(ctx.getHandle(), name());
    }
    *///?} else {
    @Override
    public Recipe<?> cast(Object parsedArgument) {
        return (Recipe<?>) parsedArgument;
    }

    @Override
    protected Recipe<?> parseImpl(CommandContext ctx) throws CommandSyntaxException {
        return ResourceLocationArgument.getRecipe(ctx.getHandle(), name());
    }
    //?}
}
