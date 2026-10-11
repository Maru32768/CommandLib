package net.kunmc.lab.commandlib.util.nms.server;

import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.SimpleCommandMap;

public abstract class NMSCraftServer extends NMSClass {
    public static NMSCraftServer create() {
        return create(Bukkit.getServer());
    }

    public static NMSCraftServer create(Server handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSCraftServer.class), Server.class)
                             .newInstance(handle);
    }

    public NMSCraftServer(Server handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract NMSDedicatedServer getServer();

    public abstract SimpleCommandMap getCommandMap();
}
