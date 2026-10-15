package dev.marulabs.commandlib.util.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.nms.world.NMSVec3D;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

public abstract class NMSArgumentVec3D extends NMSArgument<NMSVec3D> {
    public static NMSArgumentVec3D create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentVec3D.class))
                             .newInstance();
    }

    public NMSArgumentVec3D(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    /**
     * Returns the argument type. With {@code centerIntegers}, as in {@link #argument()}, an integer x or z coordinate
     * moves to the center of its block: {@code 1 64 1} is {@code 1.5 64 1.5}.
     */
    public abstract ArgumentType<?> argument(boolean centerIntegers);
}
