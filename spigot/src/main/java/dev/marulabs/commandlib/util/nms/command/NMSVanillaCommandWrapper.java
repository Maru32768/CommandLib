package dev.marulabs.commandlib.util.nms.command;

import com.mojang.brigadier.tree.CommandNode;
import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.NMSClassRegistry;
import dev.marulabs.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.command.defaults.BukkitCommand;

public abstract class NMSVanillaCommandWrapper extends NMSClass {
    public static NMSVanillaCommandWrapper create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSVanillaCommandWrapper.class))
                             .newInstance();
    }

    public NMSVanillaCommandWrapper(Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract BukkitCommand createInstance(NMSCommandDispatcher dispatcher, CommandNode<?> command);
}
