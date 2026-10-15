package dev.marulabs.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentBlockPosition;
import org.bukkit.util.BlockVector;

/**
 * Reads three space-separated integers such as {@code 1 64 -3}. Relative and local coordinates are not supported.
 */
public class MockNMSArgumentBlockPosition extends NMSArgumentBlockPosition {
    public MockNMSArgumentBlockPosition() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return reader -> {
            int x = reader.readInt();
            reader.expect(' ');
            int y = reader.readInt();
            reader.expect(' ');
            int z = reader.readInt();
            return new BlockVector(x, y, z);
        };
    }

    @Override
    protected BlockVector parseImpl(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, BlockVector.class);
    }
}
