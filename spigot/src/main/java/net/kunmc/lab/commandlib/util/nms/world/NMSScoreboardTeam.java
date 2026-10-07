package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.MinecraftClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.world.v1_16_0.NMSScoreboardTeam_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.world.v1_17_0.NMSScoreboardTeam_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.world.v1_18_0.NMSScoreboardTeam_v1_18_0;
import net.kunmc.lab.commandlib.util.nms.world.v1_20_5.NMSScoreboardTeam_v1_20_5;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSScoreboardTeam extends MinecraftClass {
    public static NMSScoreboardTeam create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSScoreboardTeam.class), Object.class)
                             .newInstance(handle);
    }

    public NMSScoreboardTeam(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    public abstract String getName();

    static {
        NMSClassRegistry.register(NMSScoreboardTeam.class, NMSScoreboardTeam_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSScoreboardTeam.class, NMSScoreboardTeam_v1_17_0.class, "1.17.0", "1.17.1");
        NMSClassRegistry.register(NMSScoreboardTeam.class, NMSScoreboardTeam_v1_18_0.class, "1.18.0", "1.20.4");
        NMSClassRegistry.register(NMSScoreboardTeam.class, NMSScoreboardTeam_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSScoreboardTeam.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSScoreboardTeam_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSScoreboardTeam.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSScoreboardTeam_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSScoreboardTeam.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSScoreboardTeam_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSScoreboardTeam.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSScoreboardTeam_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSScoreboardTeam.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_18_2.NMSScoreboardTeam_spigot_1_18_2",
                                       "1.18.2",
                                       "1.18.2");
        NMSClassRegistry.registerTyped(NMSScoreboardTeam.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_17_1.NMSScoreboardTeam_spigot_1_17_1",
                                       "1.17.1",
                                       "1.17.1");
        NMSClassRegistry.registerTyped(NMSScoreboardTeam.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSScoreboardTeam_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSScoreboardTeam.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSScoreboardTeam_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
        NMSClassRegistry.registerTyped(NMSScoreboardTeam.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_16_5.NMSScoreboardTeam_spigot_1_16_5",
                                       "1.16.4",
                                       "1.16.5");
    }
}
