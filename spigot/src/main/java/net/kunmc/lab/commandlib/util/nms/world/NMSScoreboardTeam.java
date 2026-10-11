package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSScoreboardTeam extends NMSClass {
    public static NMSScoreboardTeam create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSScoreboardTeam.class), Object.class)
                             .newInstance(handle);
    }

    public NMSScoreboardTeam(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract String getName();
}
