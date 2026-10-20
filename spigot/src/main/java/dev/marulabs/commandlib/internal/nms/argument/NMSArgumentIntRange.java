package dev.marulabs.commandlib.internal.nms.argument;

import com.google.common.collect.Range;
import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSArgumentIntRange extends NMSArgument<Range<Integer>> {
    public static NMSArgumentIntRange create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentIntRange.class))
                             .newInstance();
    }

    public NMSArgumentIntRange(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
