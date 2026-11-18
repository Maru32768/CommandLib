package dev.marulabs.cadence.internal.nms.argument;

import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSArgumentPlayer extends NMSArgument<Player> {
    public static NMSArgumentPlayer create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentPlayer.class))
                             .newInstance();
    }

    public NMSArgumentPlayer(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
