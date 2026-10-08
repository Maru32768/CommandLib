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
        NMSClassRegistry.registerTyped(NMSCraftServer.class);
    }
}
