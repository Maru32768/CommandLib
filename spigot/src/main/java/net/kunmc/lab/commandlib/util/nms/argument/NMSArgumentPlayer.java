package net.kunmc.lab.commandlib.util.nms.argument;

import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.argument.v1_16_0.NMSArgumentPlayer_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_17_0.NMSArgumentPlayer_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_20_5.NMSArgumentPlayer_v1_20_5;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.entity.Player;

public abstract class NMSArgumentPlayer extends NMSArgument<Player> {
    public static NMSArgumentPlayer create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentPlayer.class))
                             .newInstance();
    }

    public NMSArgumentPlayer(Object handle, String className, String... classNames) {
        super(handle, className, classNames);
    }

    static {
        NMSClassRegistry.register(NMSArgumentPlayer.class, NMSArgumentPlayer_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSArgumentPlayer.class, NMSArgumentPlayer_v1_17_0.class, "1.17.0", "1.20.4");
        NMSClassRegistry.register(NMSArgumentPlayer.class, NMSArgumentPlayer_v1_20_5.class, "1.20.5", "9.9.9");
        NMSClassRegistry.registerTyped(NMSArgumentPlayer.class);

    }
}
