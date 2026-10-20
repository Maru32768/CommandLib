package dev.marulabs.commandlib.internal.nms.mock.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentBlockPosition;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.ApiStatus;

/**
 * Reads three space-separated integers such as {@code 1 64 -3}. Relative and local coordinates are not supported.
 */
@ApiStatus.Internal
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
