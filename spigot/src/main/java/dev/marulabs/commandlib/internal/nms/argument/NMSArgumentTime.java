package dev.marulabs.commandlib.internal.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSArgumentTime extends NMSArgument<Integer> {
    public static NMSArgumentTime create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentTime.class))
                             .newInstance();
    }

    public NMSArgumentTime(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    /**
     * Returns the argument type that rejects fewer ticks than {@code minimum}. The type of servers before 1.19.4 has no
     * minimum and only rejects negative values, so it is {@link #argument()} there.
     */
    public abstract ArgumentType<?> argument(int minimum);
}
