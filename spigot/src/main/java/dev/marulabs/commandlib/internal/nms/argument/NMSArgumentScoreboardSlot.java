package dev.marulabs.commandlib.internal.nms.argument;

import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The vanilla scoreboard slot argument. It parses to the vanilla name of the slot, such as {@code sidebar.team.red}.
 */
@ApiStatus.Internal
public abstract class NMSArgumentScoreboardSlot extends NMSArgument<String> {
    public static NMSArgumentScoreboardSlot create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentScoreboardSlot.class))
                             .newInstance();
    }

    public NMSArgumentScoreboardSlot(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
