package net.kunmc.lab.commandlib.util.nms.server;

import net.kunmc.lab.commandlib.util.nms.CraftBukkitClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.server.v1_16_0.NMSCraftServer_v1_16_0;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Server;

public abstract class NMSCraftServer extends CraftBukkitClass {
    public static NMSCraftServer create() {
        return create(Bukkit.getServer());
    }

    public static NMSCraftServer create(Server handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftServer.class), Server.class)
                             .newInstance(handle);
    }

    public NMSCraftServer(Server handle, String className) {
        super(handle, className);
    }

    public abstract NMSDedicatedServer getServer();

    static {
        NMSClassRegistry.register(NMSCraftServer.class, NMSCraftServer_v1_16_0.class, "1.16.0", "9.9.9");
        NMSClassRegistry.registerTyped(NMSCraftServer.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSCraftServer_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSCraftServer.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSCraftServer_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSCraftServer.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSCraftServer_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSCraftServer.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSCraftServer_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSCraftServer.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_18_2.NMSCraftServer_spigot_1_18_2",
                                       "1.18.2",
                                       "1.18.2");
        NMSClassRegistry.registerTyped(NMSCraftServer.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_17_1.NMSCraftServer_spigot_1_17_1",
                                       "1.17.1",
                                       "1.17.1");
        NMSClassRegistry.registerTyped(NMSCraftServer.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSCraftServer_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSCraftServer.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSCraftServer_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
        NMSClassRegistry.registerTyped(NMSCraftServer.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_16_5.NMSCraftServer_spigot_1_16_5",
                                       "1.16.4",
                                       "1.16.5");
    }
}
