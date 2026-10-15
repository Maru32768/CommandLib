package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import org.bukkit.Location;

/**
 * A block position in the vanilla {@code block_pos} syntax: {@code x y z} integers, relative ({@code ~}) or local
 * ({@code ^}) coordinates. The client completes it with the block it looks at.
 *
 * <p>The value is the location of the block's corner in the sender's world, with integer coordinates. Neither the
 * chunk nor the world height is checked.
 */
@SuppressWarnings("UnstableApiUsage")
public class BlockPositionArgument extends Argument<Location, BlockPositionArgument> {
    public BlockPositionArgument(String name) {
        super(name, ArgumentTypes.blockPosition());
    }

    @Override
    public Location cast(Object parsedArgument) {
        return (Location) parsedArgument;
    }

    @Override
    protected Location parseImpl(CommandContext ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getHandle()
                                       .getSource();
        return ctx.getHandle()
                  .getArgument(name(), BlockPositionResolver.class)
                  .resolve(source)
                  .toLocation(source.getLocation()
                                    .getWorld());
    }
}
