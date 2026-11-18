package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import org.bukkit.entity.Entity;


@SuppressWarnings("UnstableApiUsage")
public class EntityArgument extends Argument<Entity, EntityArgument> {
    public EntityArgument(String name) {
        super(name, ArgumentTypes.entity());
    }

    @Override
    public Entity cast(Object parsedArgument) {
        return (Entity) parsedArgument;
    }

    @Override
    protected Entity parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        EntitySelectorArgumentResolver resolver = ctx.getHandle()
                                                     .getArgument(name(), EntitySelectorArgumentResolver.class);
        // Paper's entity() resolver throws vanilla's error itself; this covers a resolver that returns no entity.
        return SelectorResults.requireAny(resolver.resolve(ctx.getHandle()
                                                              .getSource()),
                                          SelectorResults.NO_ENTITY)
                              .get(0);
    }
}
