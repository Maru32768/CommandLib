package dev.marulabs.commandlib.internal.nms.argument;

import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The vanilla block position argument. The value is the position resolved against the sender, without checking that
 * it is loaded or inside the world.
 */
@ApiStatus.Internal
public abstract class NMSArgumentBlockPosition extends NMSArgument<BlockVector> {
    public static NMSArgumentBlockPosition create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentBlockPosition.class))
                             .newInstance();
    }

    public NMSArgumentBlockPosition(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
