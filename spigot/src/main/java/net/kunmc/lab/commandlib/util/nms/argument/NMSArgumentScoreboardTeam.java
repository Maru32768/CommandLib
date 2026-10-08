package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.argument.v1_16_0.NMSArgumentScoreboardTeam_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_17_0.NMSArgumentScoreboardTeam_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_20_5.NMSArgumentScoreboardTeam_v1_20_5;
import net.kunmc.lab.commandlib.util.nms.world.NMSScoreboardTeam;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentScoreboardTeam extends NMSArgument<NMSScoreboardTeam> {
    public static NMSArgumentScoreboardTeam create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentScoreboardTeam.class))
                             .newInstance();
    }

    public NMSArgumentScoreboardTeam(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    static {
        NMSClassRegistry.register(NMSArgumentScoreboardTeam.class,
                                  NMSArgumentScoreboardTeam_v1_16_0.class,
                                  "1.16.0",
                                  "1.16.5");
        NMSClassRegistry.register(NMSArgumentScoreboardTeam.class,
                                  NMSArgumentScoreboardTeam_v1_17_0.class,
                                  "1.17.0",
                                  "1.20.4");
        NMSClassRegistry.register(NMSArgumentScoreboardTeam.class,
                                  NMSArgumentScoreboardTeam_v1_20_5.class,
                                  "1.20.5",
                                  "9.9.9");
        NMSClassRegistry.registerTyped(NMSArgumentScoreboardTeam.class);
    }
}
