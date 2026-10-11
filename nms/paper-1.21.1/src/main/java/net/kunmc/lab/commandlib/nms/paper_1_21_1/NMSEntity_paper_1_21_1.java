package net.kunmc.lab.commandlib.nms.paper_1_21_1;

import net.kunmc.lab.commandlib.util.nms.world.NMSEntity;
import net.minecraft.world.entity.Entity;

public class NMSEntity_paper_1_21_1 extends NMSEntity {
    public NMSEntity_paper_1_21_1(Object handle) {
        super(handle, Entity.class);
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity() {
        return ((Entity) getHandle()).getBukkitEntity();
    }
}
