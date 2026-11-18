package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import net.minecraft.world.entity.Entity;

public class EntityArgument extends Argument<Entity, EntityArgument> {
    public EntityArgument(String name) {
        super(name, net.minecraft.commands.arguments.EntityArgument.entity());
    }

    @Override
    public Entity cast(Object parsedArgument) {
        return ((Entity) parsedArgument);
    }

    @Override
    protected Entity parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return net.minecraft.commands.arguments.EntityArgument.getEntity(ctx.getHandle(), name());
    }
}
