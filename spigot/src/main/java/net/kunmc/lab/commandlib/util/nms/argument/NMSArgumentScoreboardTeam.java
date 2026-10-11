package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.world.NMSScoreboardTeam;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentScoreboardTeam extends NMSArgument<NMSScoreboardTeam> {
    public static NMSArgumentScoreboardTeam create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentScoreboardTeam.class))
                             .newInstance();
    }

    public NMSArgumentScoreboardTeam(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
