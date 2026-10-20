package dev.marulabs.commandlib.internal.nms.world;

import dev.marulabs.commandlib.internal.nms.NMSClass;
import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSScoreboardTeam extends NMSClass {
    public static NMSScoreboardTeam create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSScoreboardTeam.class), Object.class)
                             .newInstance(handle);
    }

    public NMSScoreboardTeam(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract String getName();
}
