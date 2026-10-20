package dev.marulabs.commandlib.internal.nms.argument;

import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.nms.world.NMSScoreboardTeam;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSArgumentScoreboardTeam extends NMSArgument<NMSScoreboardTeam> {
    public static NMSArgumentScoreboardTeam create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentScoreboardTeam.class))
                             .newInstance();
    }

    public NMSArgumentScoreboardTeam(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
