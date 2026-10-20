package dev.marulabs.commandlib.internal.nms.argument;

import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@ApiStatus.Internal
public abstract class NMSArgumentPlayers extends NMSArgument<List<Player>> {
    public static NMSArgumentPlayers create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentPlayers.class))
                             .newInstance();
    }

    public NMSArgumentPlayers(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
