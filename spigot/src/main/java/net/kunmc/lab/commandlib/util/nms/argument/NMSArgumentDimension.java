package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.argument.v1_16_0.NMSArgumentDimension_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_17_0.NMSArgumentDimension_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_20_5.NMSArgumentDimension_v1_20_5;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.World;

public abstract class NMSArgumentDimension extends NMSArgument<World> {
    public static NMSArgumentDimension create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentDimension.class))
                             .newInstance();
    }

    public NMSArgumentDimension(Object handle, String className) {
        super(handle, className);
    }

    static {
        NMSClassRegistry.register(NMSArgumentDimension.class, NMSArgumentDimension_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSArgumentDimension.class, NMSArgumentDimension_v1_17_0.class, "1.17.0", "1.20.4");
        NMSClassRegistry.register(NMSArgumentDimension.class, NMSArgumentDimension_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSArgumentDimension.class);
    }
}
