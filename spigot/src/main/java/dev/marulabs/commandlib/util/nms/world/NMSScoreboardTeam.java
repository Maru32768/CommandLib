package dev.marulabs.commandlib.util.nms.world;

import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

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
