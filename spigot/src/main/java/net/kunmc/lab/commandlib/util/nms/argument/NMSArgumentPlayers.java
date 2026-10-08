package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.argument.v1_16_0.NMSArgumentPlayers_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_17_0.NMSArgumentPlayers_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_20_5.NMSArgumentPlayers_v1_20_5;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class NMSArgumentPlayers extends NMSArgument<List<Player>> {
    public static NMSArgumentPlayers create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentPlayers.class))
                             .newInstance();
    }

    public NMSArgumentPlayers(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    static {
        NMSClassRegistry.register(NMSArgumentPlayers.class, NMSArgumentPlayers_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSArgumentPlayers.class, NMSArgumentPlayers_v1_17_0.class, "1.17.0", "1.20.4");
        NMSClassRegistry.register(NMSArgumentPlayers.class, NMSArgumentPlayers_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSArgumentPlayers.class);
    }
}
