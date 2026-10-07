package net.kunmc.lab.commandlib.nms.spigot_1_18_2;

import net.kunmc.lab.commandlib.util.nms.world.NMSEntity;
import net.minecraft.world.entity.Entity;

public class NMSEntity_spigot_1_18_2 extends NMSEntity {
    public NMSEntity_spigot_1_18_2(Object handle) {
        super(handle, "world.entity.Entity");
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity() {
        return ((Entity) getHandle()).getBukkitEntity();
    }
}
