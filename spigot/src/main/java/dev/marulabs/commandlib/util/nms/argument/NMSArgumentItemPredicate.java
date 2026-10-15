package dev.marulabs.commandlib.util.nms.argument;

import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * The vanilla item predicate argument. The predicate tests a Bukkit item through a server copy of it.
 */
public abstract class NMSArgumentItemPredicate extends NMSArgument<Predicate<ItemStack>> {
    public static NMSArgumentItemPredicate create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentItemPredicate.class))
                             .newInstance();
    }

    public NMSArgumentItemPredicate(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
