package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.MinecraftClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.world.v1_16_0.NMSMobEffectList_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.world.v1_17_0.NMSMobEffectList_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.world.v1_20_5.NMSMobEffectList_v1_20_5;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSMobEffectList extends MinecraftClass {
    public static NMSMobEffectList create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSMobEffectList.class), Object.class)
                             .newInstance(handle);
    }

    public NMSMobEffectList(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    static {
        NMSClassRegistry.register(NMSMobEffectList.class, NMSMobEffectList_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSMobEffectList.class, NMSMobEffectList_v1_17_0.class, "1.17.0", "1.20.4");
        NMSClassRegistry.register(NMSMobEffectList.class, NMSMobEffectList_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSMobEffectList.class);
    }
}
