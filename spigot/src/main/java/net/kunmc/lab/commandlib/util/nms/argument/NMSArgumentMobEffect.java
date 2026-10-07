package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.argument.v1_16_0.NMSArgumentMobEffect_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_17_0.NMSArgumentMobEffect_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_18_0.NMSArgumentMobEffect_v1_18_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_19_3.NMSArgumentMobEffect_v1_19_3;
import net.kunmc.lab.commandlib.util.nms.argument.v1_20_5.NMSArgumentMobEffect_v1_20_5;
import net.kunmc.lab.commandlib.util.nms.world.NMSMobEffectList;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentMobEffect extends NMSArgument<NMSMobEffectList> {
    public static NMSArgumentMobEffect create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentMobEffect.class))
                             .newInstance();
    }

    public NMSArgumentMobEffect(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    static {
        NMSClassRegistry.register(NMSArgumentMobEffect.class, NMSArgumentMobEffect_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSArgumentMobEffect.class, NMSArgumentMobEffect_v1_17_0.class, "1.17.0", "1.17.1");
        NMSClassRegistry.register(NMSArgumentMobEffect.class, NMSArgumentMobEffect_v1_18_0.class, "1.18.0", "1.19.2");
        NMSClassRegistry.register(NMSArgumentMobEffect.class, NMSArgumentMobEffect_v1_19_3.class, "1.19.3", "1.20.4");
        NMSClassRegistry.register(NMSArgumentMobEffect.class, NMSArgumentMobEffect_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSArgumentMobEffect.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSArgumentMobEffect_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSArgumentMobEffect.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSArgumentMobEffect_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSArgumentMobEffect.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSArgumentMobEffect_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSArgumentMobEffect.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSArgumentMobEffect_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSArgumentMobEffect.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_18_2.NMSArgumentMobEffect_spigot_1_18_2",
                                       "1.18.2",
                                       "1.18.2");
        NMSClassRegistry.registerTyped(NMSArgumentMobEffect.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_17_1.NMSArgumentMobEffect_spigot_1_17_1",
                                       "1.17.1",
                                       "1.17.1");
        NMSClassRegistry.registerTyped(NMSArgumentMobEffect.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSArgumentMobEffect_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSArgumentMobEffect.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSArgumentMobEffect_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
        NMSClassRegistry.registerTyped(NMSArgumentMobEffect.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_16_5.NMSArgumentMobEffect_spigot_1_16_5",
                                       "1.16.4",
                                       "1.16.5");
    }
}
