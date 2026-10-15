package dev.marulabs.commandlib.nms.spigot_1_19_2;

import dev.marulabs.commandlib.util.nms.world.NMSEntity;
import net.minecraft.world.entity.Entity;

public class NMSEntity_spigot_1_19_2 extends NMSEntity {
    public NMSEntity_spigot_1_19_2(Object handle) {
        super(handle, Entity.class);
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity() {
        return ((Entity) requireHandle()).getBukkitEntity();
    }
}
