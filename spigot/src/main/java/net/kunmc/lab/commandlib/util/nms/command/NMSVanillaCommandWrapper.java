package net.kunmc.lab.commandlib.util.nms.command;

import com.mojang.brigadier.tree.CommandNode;
import net.kunmc.lab.commandlib.util.nms.CraftBukkitClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.command.v1_16_0.NMSVanillaCommandWrapper_v1_16_0;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.command.defaults.BukkitCommand;

public abstract class NMSVanillaCommandWrapper extends CraftBukkitClass {
    public static NMSVanillaCommandWrapper create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSVanillaCommandWrapper.class))
                             .newInstance();
    }

    public NMSVanillaCommandWrapper(Object handle, String className) {
        super(handle, className);
    }

    public abstract BukkitCommand createInstance(NMSCommandDispatcher dispatcher, CommandNode<?> command);

    static {
        NMSClassRegistry.register(NMSVanillaCommandWrapper.class,
                                  NMSVanillaCommandWrapper_v1_16_0.class,
                                  "1.16.0",
                                  "9.9.9");
        NMSClassRegistry.registerTyped(NMSVanillaCommandWrapper.class);
    }
}
