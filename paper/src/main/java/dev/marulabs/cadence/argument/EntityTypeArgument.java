package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.entity.EntityType;

@SuppressWarnings("UnstableApiUsage")
public class EntityTypeArgument extends Argument<EntityType, EntityTypeArgument> {
    public EntityTypeArgument(String name) {
        super(name, ArgumentTypes.resource(RegistryKey.ENTITY_TYPE));
    }

    @Override
    public EntityType cast(Object parsedArgument) {
        return (EntityType) parsedArgument;
    }

    @Override
    protected EntityType parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ctx.getHandle()
                  .getArgument(name(), EntityType.class);
    }
}
