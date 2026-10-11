package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.World;

public abstract class NMSArgumentDimension extends NMSArgument<World> {
    public static NMSArgumentDimension create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentDimension.class))
                             .newInstance();
    }

    public NMSArgumentDimension(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
