package dev.marulabs.commandlib.nms.spigot_1_16_5;

import dev.marulabs.commandlib.util.nms.world.NMSEntity;
import net.minecraft.server.v1_16_R3.Entity;

public class NMSEntity_spigot_1_16_5 extends NMSEntity {
    public NMSEntity_spigot_1_16_5(Object handle) {
        super(handle, Entity.class);
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity() {
        return ((Entity) getHandle()).getBukkitEntity();
    }
}
