package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import dev.marulabs.cadence.internal.nms.argument.NMSArgumentEntity;
import org.bukkit.entity.Entity;

public class EntityArgument extends Argument<Entity, EntityArgument> {
    public EntityArgument(String name) {
        super(name,
              NMSArgumentEntity.create()
                               .argument());
    }

    @Override
    public Entity cast(Object parsedArgument) {
        return ((Entity) parsedArgument);
    }

    @Override
    protected Entity parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return NMSArgumentEntity.create()
                                .parse(ctx.getHandle(), name());
    }
}
