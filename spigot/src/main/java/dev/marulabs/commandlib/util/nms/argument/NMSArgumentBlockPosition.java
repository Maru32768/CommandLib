package dev.marulabs.commandlib.util.nms.argument;

import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Nullable;

/**
 * The vanilla block position argument. The value is the position resolved against the sender, without checking that
 * it is loaded or inside the world.
 */
public abstract class NMSArgumentBlockPosition extends NMSArgument<BlockVector> {
    public static NMSArgumentBlockPosition create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentBlockPosition.class))
                             .newInstance();
    }

    public NMSArgumentBlockPosition(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
