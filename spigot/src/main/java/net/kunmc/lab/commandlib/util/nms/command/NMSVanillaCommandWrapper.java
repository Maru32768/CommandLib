package net.kunmc.lab.commandlib.util.nms.command;

import com.mojang.brigadier.tree.CommandNode;
import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
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
