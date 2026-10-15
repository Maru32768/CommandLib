package dev.marulabs.commandlib.util.nms.argument;

import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

/**
 * The vanilla scoreboard slot argument. It parses to the vanilla name of the slot, such as {@code sidebar.team.red}.
 */
public abstract class NMSArgumentScoreboardSlot extends NMSArgument<String> {
    public static NMSArgumentScoreboardSlot create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentScoreboardSlot.class))
                             .newInstance();
    }

    public NMSArgumentScoreboardSlot(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
