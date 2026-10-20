package dev.marulabs.commandlib.internal.nms.argument;

import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.bukkit.block.Block;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * The vanilla block predicate argument. The predicate tests a Bukkit block in its world and loads its chunk when
 * needed, as {@code /execute if block} does.
 */
@ApiStatus.Internal
public abstract class NMSArgumentBlockPredicate extends NMSArgument<Predicate<Block>> {
    public static NMSArgumentBlockPredicate create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentBlockPredicate.class))
                             .newInstance();
    }

    public NMSArgumentBlockPredicate(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
