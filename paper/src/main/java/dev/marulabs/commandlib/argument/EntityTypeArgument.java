package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
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
