package dev.marulabs.commandlib.internal.nms.world;

import dev.marulabs.commandlib.internal.nms.NMSClass;
import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSCraftEnchantment extends NMSClass {
    public static NMSCraftEnchantment create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftEnchantment.class))
                             .newInstance();
    }

    public NMSCraftEnchantment(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract Enchantment createInstance(NMSEnchantment nms);
}
