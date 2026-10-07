package net.kunmc.lab.commandlib.util.nms.command;

import net.kunmc.lab.commandlib.util.nms.MinecraftClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.command.v1_16_0.NMSCommandListenerWrapper_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.command.v1_17_0.NMSCommandListenerWrapper_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.command.v1_20_5.NMSCommandListenerWrapper_v1_20_5;
import net.kunmc.lab.commandlib.util.nms.command.v1_20_6.NMSCommandListenerWrapper_v1_20_6;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public abstract class NMSCommandListenerWrapper extends MinecraftClass {
    public static NMSCommandListenerWrapper create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCommandListenerWrapper.class), Object.class)
                             .newInstance(handle);
    }

    public NMSCommandListenerWrapper(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    public abstract CommandSender getBukkitSender();

    public abstract Entity getBukkitEntity();

    public abstract World getBukkitWorld();

    public abstract Location getBukkitLocation();

    static {
        NMSClassRegistry.register(NMSCommandListenerWrapper.class,
                                  NMSCommandListenerWrapper_v1_16_0.class,
                                  "1.16.0",
                                  "1.16.5");
        NMSClassRegistry.register(NMSCommandListenerWrapper.class,
                                  NMSCommandListenerWrapper_v1_17_0.class,
                                  "1.17.0",
                                  "1.20.4");
        NMSClassRegistry.register(NMSCommandListenerWrapper.class,
                                  NMSCommandListenerWrapper_v1_20_5.class,
                                  "1.20.5",
                                  "1.20.5");
        NMSClassRegistry.register(NMSCommandListenerWrapper.class,
                                  NMSCommandListenerWrapper_v1_20_6.class,
                                  "1.20.6",
                                  "9.9.9");
        NMSClassRegistry.registerTyped(NMSCommandListenerWrapper.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSCommandListenerWrapper_paper_1_20_6",
                                       "1.20.6",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSCommandListenerWrapper.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSCommandListenerWrapper_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSCommandListenerWrapper.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSCommandListenerWrapper_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSCommandListenerWrapper.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSCommandListenerWrapper_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSCommandListenerWrapper.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_18_2.NMSCommandListenerWrapper_spigot_1_18_2",
                                       "1.18.2",
                                       "1.18.2");
        NMSClassRegistry.registerTyped(NMSCommandListenerWrapper.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_17_1.NMSCommandListenerWrapper_spigot_1_17_1",
                                       "1.17.1",
                                       "1.17.1");
        NMSClassRegistry.registerTyped(NMSCommandListenerWrapper.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSCommandListenerWrapper_paper_1_20_6",
                                       "1.20.6",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSCommandListenerWrapper.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSCommandListenerWrapper_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
        NMSClassRegistry.registerTyped(NMSCommandListenerWrapper.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_16_5.NMSCommandListenerWrapper_spigot_1_16_5",
                                       "1.16.4",
                                       "1.16.5");
    }
}
