package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import org.bukkit.entity.Entity;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class EntitiesArgument extends Argument<List<Entity>, EntitiesArgument> {
    public EntitiesArgument(String name) {
        super(name, ArgumentTypes.entities());
    }

    @Override
    public List<Entity> cast(Object parsedArgument) {
        return (List<Entity>) parsedArgument;
    }

    @Override
    protected List<Entity> parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        EntitySelectorArgumentResolver resolver = ctx.getHandle()
                                                     .getArgument(name(), EntitySelectorArgumentResolver.class);
        return SelectorResults.requireAny(resolver.resolve(ctx.getHandle()
                                                              .getSource()),
                                          SelectorResults.NO_ENTITY);
    }
}
