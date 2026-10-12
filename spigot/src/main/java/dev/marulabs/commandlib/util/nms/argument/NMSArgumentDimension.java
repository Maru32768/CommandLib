package dev.marulabs.commandlib.util.nms.argument;

import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

public abstract class NMSArgumentDimension extends NMSArgument<World> {
    public static NMSArgumentDimension create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentDimension.class))
                             .newInstance();
    }

    public NMSArgumentDimension(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
