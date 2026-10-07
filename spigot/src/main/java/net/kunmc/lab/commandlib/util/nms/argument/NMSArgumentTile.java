package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.argument.v1_16_0.NMSArgumentTile_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_17_0.NMSArgumentTile_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_19_3.NMSArgumentTile_v1_19_3;
import net.kunmc.lab.commandlib.util.nms.argument.v1_20_5.NMSArgumentTile_v1_20_5;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;

public abstract class NMSArgumentTile extends NMSArgument<NMSArgumentTileLocation> {
    public static NMSArgumentTile create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentTile.class))
                             .newInstance();
    }

    public NMSArgumentTile(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    static {
        NMSClassRegistry.register(NMSArgumentTile.class, NMSArgumentTile_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSArgumentTile.class, NMSArgumentTile_v1_17_0.class, "1.17.0", "1.18.2");
        // Block state arguments need a CommandBuildContext from 1.19.0, the same constructor the 1.19.3 class uses.
        NMSClassRegistry.register(NMSArgumentTile.class, NMSArgumentTile_v1_19_3.class, "1.19.0", "1.20.4");
        NMSClassRegistry.register(NMSArgumentTile.class, NMSArgumentTile_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSArgumentTile.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6_spigot.NMSArgumentTile_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSArgumentTile.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_1.NMSArgumentTile_spigot_1_20_1",
                                       "1.20.1",
                                       "1.20.1");
        NMSClassRegistry.registerTyped(NMSArgumentTile.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_4.NMSArgumentTile_spigot_1_19_4",
                                       "1.19.4",
                                       "1.19.4");
        NMSClassRegistry.registerTyped(NMSArgumentTile.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_19_2.NMSArgumentTile_spigot_1_19_2",
                                       "1.19.2",
                                       "1.19.2");
        NMSClassRegistry.registerTyped(NMSArgumentTile.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_18_2.NMSArgumentTile_spigot_1_18_2",
                                       "1.18.2",
                                       "1.18.2");
        NMSClassRegistry.registerTyped(NMSArgumentTile.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_17_1.NMSArgumentTile_spigot_1_17_1",
                                       "1.17.1",
                                       "1.17.1");
        NMSClassRegistry.registerTyped(NMSArgumentTile.class,
                                       "net.kunmc.lab.commandlib.nms.paper_1_20_6.NMSArgumentTile_paper_1_20_6",
                                       "1.20.5",
                                       "1.20.6");
        NMSClassRegistry.registerTyped(NMSArgumentTile.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_20_4.NMSArgumentTile_spigot_1_20_4",
                                       "1.20.4",
                                       "1.20.4");
        NMSClassRegistry.registerTyped(NMSArgumentTile.class,
                                       "net.kunmc.lab.commandlib.nms.spigot_1_16_5.NMSArgumentTile_spigot_1_16_5",
                                       "1.16.4",
                                       "1.16.5");
    }
}
