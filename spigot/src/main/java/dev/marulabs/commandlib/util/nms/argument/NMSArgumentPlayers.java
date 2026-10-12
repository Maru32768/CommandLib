package dev.marulabs.commandlib.util.nms.argument;

import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class NMSArgumentPlayers extends NMSArgument<List<Player>> {
    public static NMSArgumentPlayers create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentPlayers.class))
                             .newInstance();
    }

    public NMSArgumentPlayers(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
