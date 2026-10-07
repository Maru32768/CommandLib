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
        NMSClassRegistry.registerTyped(NMSVanillaCommandWrapper.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSVanillaCommandWrapper_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSVanillaCommandWrapper.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSVanillaCommandWrapper_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSVanillaCommandWrapper.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSVanillaCommandWrapper_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSVanillaCommandWrapper.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSVanillaCommandWrapper_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSVanillaCommandWrapper.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_18_2.NMSVanillaCommandWrapper_spigot_1_18_2",
                                       "1.18.2",
                                       "1.18.2");
        NMSClassRegistry.registerTyped(NMSVanillaCommandWrapper.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_17_1.NMSVanillaCommandWrapper_spigot_1_17_1",
                                       "1.17.1",
                                       "1.17.1");
        NMSClassRegistry.registerTyped(NMSVanillaCommandWrapper.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSVanillaCommandWrapper_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSVanillaCommandWrapper.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSVanillaCommandWrapper_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
        NMSClassRegistry.registerTyped(NMSVanillaCommandWrapper.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_16_5.NMSVanillaCommandWrapper_spigot_1_16_5",
                                       "1.16.4",
                                       "1.16.5");
    }
}
