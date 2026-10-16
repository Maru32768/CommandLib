package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
//? if >=1.21.2 {
/*import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
*///?} else
import net.minecraft.commands.arguments.ResourceLocationArgument;
//? if >=1.20.2 {
/*import net.minecraft.world.item.crafting.RecipeHolder;
*///?} else
import net.minecraft.world.item.crafting.Recipe;

// A recipe of the server by its key, such as minecraft:oak_planks; the loaded recipes are suggested, and an unknown
// key fails with vanilla's error when the command runs. The value is vanilla's: a RecipeHolder from 1.20.2, which
// ServerPlayer#awardRecipes takes there, and the Recipe before. From 1.21.2 recipes are keyed by a ResourceKey, which
// vanilla reads with ResourceKeyArgument.
//? if >=1.20.2 {
/*public class RecipeArgument extends Argument<RecipeHolder<?>, RecipeArgument> {
*///?} else
public class RecipeArgument extends Argument<Recipe<?>, RecipeArgument> {
    public RecipeArgument(String name) {
        //? if >=1.21.2 {
        /*super(name, ResourceKeyArgument.key(Registries.RECIPE));
        addSuggestionProvider(sb -> RegistryEntries.suggest(sb,
                                                            RegistryEntries.server(sb.getContext())
                                                                           .getRecipeManager()
                                                                           .getRecipes()
                                                                           .stream()
                                                                           .map(x -> x.id()
                                                                                      .location())));
        *///?} else {
        super(name, ResourceLocationArgument.id());
        addSuggestionProvider(sb -> RegistryEntries.suggest(sb,
                                                            sb.getContext()
                                                              .getHandle()
                                                              .getSource()
                                                              .getRecipeNames()));
        //?}
    }

    //? if >=1.21.2 {
    /*// Vanilla's error of ResourceKeyArgument.getRecipe.
    private static final DynamicCommandExceptionType UNKNOWN_RECIPE =
            new DynamicCommandExceptionType(id -> Component.translatableEscape("recipe.notFound", id));

    @Override
    public RecipeHolder<?> cast(Object parsedArgument) {
        return (RecipeHolder<?>) parsedArgument;
    }

    // NeoForge's ResourceKeyArgument.getRecipe asks the source's level whether it runs on a client, which fails for a
    // source without a level, so look the key up in the server's recipes as vanilla does.
    @Override
    @SuppressWarnings("unchecked")
    protected RecipeHolder<?> parseImpl(CommandContext ctx) throws CommandSyntaxException {
        ResourceKey<Recipe<?>> key = (ResourceKey<Recipe<?>>) ctx.getHandle()
                                                                .getArgument(name(), ResourceKey.class);
        return RegistryEntries.server(ctx)
                              .getRecipeManager()
                              .byKey(key)
                              .orElseThrow(() -> UNKNOWN_RECIPE.create(key.location()));
    }
    *///?} elif >=1.20.2 {
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
