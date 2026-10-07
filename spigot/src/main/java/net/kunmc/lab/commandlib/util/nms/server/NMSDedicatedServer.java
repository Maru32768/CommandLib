package net.kunmc.lab.commandlib.util.nms.server;

import net.kunmc.lab.commandlib.util.nms.MinecraftClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.command.NMSCommandDispatcher;
import net.kunmc.lab.commandlib.util.nms.server.v1_16_0.NMSDedicatedServer_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.server.v1_17_0.NMSDedicatedServer_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.server.v1_18_0.NMSDedicatedServer_v1_18_0;
import net.kunmc.lab.commandlib.util.nms.server.v1_19_0.NMSDedicatedServer_v1_19_0;
import net.kunmc.lab.commandlib.util.nms.server.v1_20_4.NMSDedicatedServer_v1_20_4;
import net.kunmc.lab.commandlib.util.nms.server.v1_20_5.NMSDedicatedServer_v1_20_5;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSDedicatedServer extends MinecraftClass {
    public static NMSDedicatedServer create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSDedicatedServer.class), Object.class)
                             .newInstance(handle);
    }

    public NMSDedicatedServer(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    public abstract NMSCommandDispatcher getCommandDispatcher();

    public abstract NMSDataPackResources getDataPackResources();

    static {
        NMSClassRegistry.register(NMSDedicatedServer.class, NMSDedicatedServer_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSDedicatedServer.class, NMSDedicatedServer_v1_17_0.class, "1.17.0", "1.17.1");
        NMSClassRegistry.register(NMSDedicatedServer.class, NMSDedicatedServer_v1_18_0.class, "1.18.0", "1.18.2");
        NMSClassRegistry.register(NMSDedicatedServer.class, NMSDedicatedServer_v1_19_0.class, "1.19.0", "1.20.3");
        NMSClassRegistry.register(NMSDedicatedServer.class, NMSDedicatedServer_v1_20_4.class, "1.20.4", "1.20.4");
        NMSClassRegistry.register(NMSDedicatedServer.class, NMSDedicatedServer_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSDedicatedServer.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSDedicatedServer_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSDedicatedServer.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSDedicatedServer_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSDedicatedServer.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSDedicatedServer_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSDedicatedServer.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSDedicatedServer_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSDedicatedServer.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_18_2.NMSDedicatedServer_spigot_1_18_2",
                                       "1.18.2",
                                       "1.18.2");
        NMSClassRegistry.registerTyped(NMSDedicatedServer.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_17_1.NMSDedicatedServer_spigot_1_17_1",
                                       "1.17.1",
                                       "1.17.1");
        NMSClassRegistry.registerTyped(NMSDedicatedServer.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSDedicatedServer_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSDedicatedServer.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSDedicatedServer_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
        NMSClassRegistry.registerTyped(NMSDedicatedServer.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_16_5.NMSDedicatedServer_spigot_1_16_5",
                                       "1.16.4",
                                       "1.16.5");
    }

    /**
     * for after 1.19.0
     */
    public static abstract class NMSReloadableResources extends MinecraftClass {
        public static NMSReloadableResources create(Object handle) {
            return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSReloadableResources.class), Object.class)
                                 .newInstance(handle);
        }

        public NMSReloadableResources(Object handle, String className, String... classNames) {
            super(handle, className, classNames);
        }

        public abstract NMSDataPackResources getDataPackResources();

        static {
            NMSClassRegistry.register(NMSDedicatedServer.NMSReloadableResources.class,
                                      NMSDedicatedServer_v1_19_0.NMSReloadableResources_v1_19_0.class,
                                      "1.19.0",
                                      "1.20.3");
            NMSClassRegistry.register(NMSDedicatedServer.NMSReloadableResources.class,
                                      NMSDedicatedServer_v1_20_4.NMSReloadableResources_v1_20_4.class,
                                      "1.20.4",
                                      "1.20.4");
            NMSClassRegistry.register(NMSDedicatedServer.NMSReloadableResources.class,
                                      NMSDedicatedServer_v1_20_5.NMSReloadableResources_v1_20_5.class,
                                      "1.20.5",
                                      "9.9.9");
        }
    }
}
