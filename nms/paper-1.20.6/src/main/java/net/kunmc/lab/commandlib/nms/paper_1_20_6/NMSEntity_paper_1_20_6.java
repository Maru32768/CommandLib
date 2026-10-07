package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import net.kunmc.lab.commandlib.util.nms.world.NMSEntity;
import net.minecraft.world.entity.Entity;

public class NMSEntity_paper_1_20_6 extends NMSEntity {
    public NMSEntity_paper_1_20_6(Object handle) {
        super(handle, "world.entity.Entity");
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity() {
        return ((Entity) getHandle()).getBukkitEntity();
    }
}
