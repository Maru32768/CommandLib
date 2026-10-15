package dev.marulabs.commandlib.util.nms.argument;

import com.google.common.collect.Range;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

public abstract class NMSArgumentIntRange extends NMSArgument<Range<Integer>> {
    public static NMSArgumentIntRange create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentIntRange.class))
                             .newInstance();
    }

    public NMSArgumentIntRange(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
