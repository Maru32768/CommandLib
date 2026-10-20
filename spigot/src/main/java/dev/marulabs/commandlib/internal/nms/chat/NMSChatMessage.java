package dev.marulabs.commandlib.internal.nms.chat;

import com.mojang.brigadier.Message;
import dev.marulabs.commandlib.internal.bukkit.BukkitUtil;
import dev.marulabs.commandlib.internal.bukkit.MinecraftVersion;
import dev.marulabs.commandlib.internal.nms.NMSClass;
import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;

/**
 * For 1.18.2 and earlier, which isSupportedVersion() tells apart.
 */
@ApiStatus.Internal
public abstract class NMSChatMessage extends NMSClass {
    public static boolean isSupportedVersion() {
        return new MinecraftVersion(BukkitUtil.getMinecraftVersion()).isLessThan(new MinecraftVersion("1.19.0"));
    }

    public static NMSChatMessage create(Message handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSChatMessage.class), Message.class)
                             .newInstance(handle);
    }

    public NMSChatMessage(Message handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract String getKey();

    public abstract Object[] getArgs();
}
