package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.world.NMSEnchantment;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentEnchantment extends NMSArgument<NMSEnchantment> {
    public static NMSArgumentEnchantment create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentEnchantment.class))
                             .newInstance();
    }

    public NMSArgumentEnchantment(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
