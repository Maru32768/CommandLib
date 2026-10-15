package dev.marulabs.commandlib.argument;

//? if >=1.19.3
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
//? if >=1.19.3 {
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;
//?} else {
/*import dev.marulabs.commandlib.Caption;
import net.minecraft.core.Registry;
*///?}
import net.minecraft.world.entity.EntityType;

/**
 * An entity type by its key, such as {@code minecraft:zombie}. Every entity type is accepted, not only the summonable
 * ones; the registry's keys are suggested.
 */
public class EntityTypeArgument extends Argument<EntityType<?>, EntityTypeArgument> {
    public EntityTypeArgument(String name) {
        //? if >=1.19.3 {
        super(name, ResourceArgument.resource(BuiltInCommandBuildContext.INSTANCE, Registries.ENTITY_TYPE));
        //?} else {
        /*super(name, net.minecraft.commands.arguments.ResourceLocationArgument.id());
        addSuggestionProvider(sb -> RegistryEntries.suggestKeys(sb, Registry.ENTITY_TYPE_REGISTRY));
        *///?}
    }

    @Override
    public EntityType<?> cast(Object parsedArgument) {
        return ((EntityType<?>) parsedArgument);
    }

    //? if >=1.19.3 {
    @Override
    protected EntityType<?> parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ResourceArgument.getResource(ctx.getHandle(), name(), Registries.ENTITY_TYPE)
                               .value();
    }
    //?} else {
    /*@Override
    protected EntityType<?> parseImpl(CommandContext ctx) throws ArgumentParseException {
        return RegistryEntries.get(ctx, name(), Registry.ENTITY_TYPE_REGISTRY, Caption.UNKNOWN_ENTITY_TYPE);
    }
    *///?}
}
