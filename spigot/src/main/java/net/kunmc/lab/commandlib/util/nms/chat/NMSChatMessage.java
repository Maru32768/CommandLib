package net.kunmc.lab.commandlib.util.nms.chat;

import com.mojang.brigadier.Message;
import net.kunmc.lab.commandlib.util.bukkit.BukkitUtil;
import net.kunmc.lab.commandlib.util.bukkit.MinecraftVersion;
import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

/**
 * for before 1.18.2
 */
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
