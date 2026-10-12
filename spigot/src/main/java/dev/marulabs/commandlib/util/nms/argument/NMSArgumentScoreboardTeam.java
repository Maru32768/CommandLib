package dev.marulabs.commandlib.util.nms.argument;

import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.nms.world.NMSScoreboardTeam;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

public abstract class NMSArgumentScoreboardTeam extends NMSArgument<NMSScoreboardTeam> {
    public static NMSArgumentScoreboardTeam create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentScoreboardTeam.class))
                             .newInstance();
    }

    public NMSArgumentScoreboardTeam(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
