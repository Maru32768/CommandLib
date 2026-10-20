package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentBlockPosition;
import dev.marulabs.commandlib.internal.nms.command.NMSCommandListenerWrapper;
import org.bukkit.Location;
import org.bukkit.util.BlockVector;

/**
 * A block position in the vanilla {@code block_pos} syntax: {@code x y z} integers, relative ({@code ~}) or local
 * ({@code ^}) coordinates. The client completes it with the block it looks at.
 *
 * <p>The value is the location of the block's corner in the sender's world, with integer coordinates. Neither the
 * chunk nor the world height is checked.
 */
public class BlockPositionArgument extends Argument<Location, BlockPositionArgument> {
    public BlockPositionArgument(String name) {
        super(name,
              NMSArgumentBlockPosition.create()
                                      .argument());
    }

    @Override
    public Location cast(Object parsedArgument) {
        return (Location) parsedArgument;
    }

    @Override
    protected Location parseImpl(CommandContext ctx) throws CommandSyntaxException {
        BlockVector pos = NMSArgumentBlockPosition.create()
                                                  .parse(ctx.getHandle(), name());
        return pos.toLocation(NMSCommandListenerWrapper.create(ctx.getHandle()
                                                                  .getSource())
                                                       .getBukkitWorld());
    }
}
