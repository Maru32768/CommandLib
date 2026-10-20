package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentEntities;
import org.bukkit.entity.Entity;

import java.util.List;

public class EntitiesArgument extends Argument<List<Entity>, EntitiesArgument> {
    public EntitiesArgument(String name) {
        super(name,
              NMSArgumentEntities.create()
                                 .argument());
    }

    @Override
    public List<Entity> cast(Object parsedArgument) {
        return ((List<Entity>) parsedArgument);
    }

    @Override
    protected List<Entity> parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return NMSArgumentEntities.create()
                                  .parse(ctx.getHandle(), name());
    }
}
