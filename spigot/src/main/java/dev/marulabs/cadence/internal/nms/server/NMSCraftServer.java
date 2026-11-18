package dev.marulabs.cadence.internal.nms.server;

import dev.marulabs.cadence.internal.nms.NMSClass;
import dev.marulabs.cadence.internal.nms.NMSClassRegistry;
import dev.marulabs.cadence.internal.reflection.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.SimpleCommandMap;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
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
