package net.kunmc.lab.commandlib.util.nms.world;

import net.kunmc.lab.commandlib.util.nms.MinecraftClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.kunmc.lab.commandlib.util.nms.world.v1_16_0.NMSEntity_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.world.v1_17_0.NMSEntity_v1_17_0;
import net.kunmc.lab.commandlib.util.reflection.ReflectionUtil;
import org.bukkit.entity.Entity;

public abstract class NMSEntity extends MinecraftClass {
    public static NMSEntity create(Object handle) {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSEntity.class), Object.class)
                             .newInstance(handle);
    }

    public NMSEntity(Object handle, String className) {
        super(handle, className);
    }

    public abstract Entity getBukkitEntity();

    static {
        NMSClassRegistry.register(NMSEntity.class, NMSEntity_v1_16_0.class, "1.16.0", "1.16.5");
        NMSClassRegistry.register(NMSEntity.class, NMSEntity_v1_17_0.class, "1.17.0", "9.9.9");
        NMSClassRegistry.registerTyped(NMSEntity.class);
    }
}
