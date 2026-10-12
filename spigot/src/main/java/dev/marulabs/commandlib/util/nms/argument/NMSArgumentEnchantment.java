package dev.marulabs.commandlib.util.nms.argument;

import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.nms.world.NMSEnchantment;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentEnchantment extends NMSArgument<NMSEnchantment> {
    public static NMSArgumentEnchantment create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentEnchantment.class))
                             .newInstance();
    }

    public NMSArgumentEnchantment(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
