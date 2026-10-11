package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentProfile extends NMSArgument<Object> {
    public static NMSArgumentProfile create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentProfile.class))
                             .newInstance();
    }

    public NMSArgumentProfile(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
