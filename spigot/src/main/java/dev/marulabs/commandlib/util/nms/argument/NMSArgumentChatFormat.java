package dev.marulabs.commandlib.util.nms.argument;

import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;

public abstract class NMSArgumentChatFormat extends NMSArgument<ChatColor> {
    public static NMSArgumentChatFormat create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentChatFormat.class))
                             .newInstance();
    }

    public NMSArgumentChatFormat(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }
}
