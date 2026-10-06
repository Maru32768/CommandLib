package net.kunmc.lab.commandlib.util.nms.server.v1_19_0;

import net.kunmc.lab.commandlib.util.nms.command.NMSCommandDispatcher;
import net.kunmc.lab.commandlib.util.nms.server.NMSDataPackResources;
import net.kunmc.lab.commandlib.util.nms.server.NMSDedicatedServer;

import java.lang.reflect.Field;

public class NMSDedicatedServer_v1_19_0 extends NMSDedicatedServer {
    public NMSDedicatedServer_v1_19_0(Object handle) {
        super(handle, "server.dedicated.DedicatedServer");
    }

    @Override
    public NMSCommandDispatcher getCommandDispatcher() {
        return NMSCommandDispatcher.create(invokeMethod("getCommands", "aC"));
    }

    @Override
    public NMSDataPackResources getDataPackResources() {
        return NMSReloadableResources.create(findReloadableResources())
                                     .getDataPackResources();
    }

    private Object findReloadableResources() {
        // The obfuscated field name changes between 1.19.x releases ("at" in 1.19.2, "au" in 1.19.4), so the field is
        // found by its type instead.
        for (Class<?> type = getHandle().getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType()
                         .getName()
                         .endsWith("MinecraftServer$ReloadableResources")) {
                    try {
                        field.setAccessible(true);
                        return field.get(getHandle());
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        throw new IllegalStateException("MinecraftServer$ReloadableResources field was not found");
    }

    public static class NMSReloadableResources_v1_19_0 extends NMSDedicatedServer.NMSReloadableResources {
        public NMSReloadableResources_v1_19_0(Object handle) {
            super(handle, "server.MinecraftServer$ReloadableResources");
        }

        @Override
        public NMSDataPackResources getDataPackResources() {
            return NMSDataPackResources.create(invokeMethod("b", "managers"));
        }
    }
}
