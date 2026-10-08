package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.argument.v1_16_0.NMSArgumentEnchantment_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_17_0.NMSArgumentEnchantment_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_19_3.NMSArgumentEnchantment_v1_19_3;
import net.kunmc.lab.commandlib.util.nms.argument.v1_20_5.NMSArgumentEnchantment_v1_20_5;
import net.kunmc.lab.commandlib.util.nms.world.NMSEnchantment;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentEnchantment extends NMSArgument<NMSEnchantment> {
    public static NMSArgumentEnchantment create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentEnchantment.class))
                             .newInstance();
    }

    public NMSArgumentEnchantment(Object handle, String className) {
        super(handle, className);
    }

    static {
        NMSClassRegistry.register(NMSArgumentEnchantment.class,
                                  NMSArgumentEnchantment_v1_16_0.class,
                                  "1.16.0",
                                  "1.16.5");
        NMSClassRegistry.register(NMSArgumentEnchantment.class,
                                  NMSArgumentEnchantment_v1_17_0.class,
                                  "1.17.0",
                                  "1.19.2");
        NMSClassRegistry.register(NMSArgumentEnchantment.class,
                                  NMSArgumentEnchantment_v1_19_3.class,
                                  "1.19.3",
                                  "1.20.4");
        NMSClassRegistry.register(NMSArgumentEnchantment.class,
                                  NMSArgumentEnchantment_v1_20_5.class,
                                  "1.20.5",
                                  "9.9.9");
        NMSClassRegistry.registerTyped(NMSArgumentEnchantment.class);
    }
}
