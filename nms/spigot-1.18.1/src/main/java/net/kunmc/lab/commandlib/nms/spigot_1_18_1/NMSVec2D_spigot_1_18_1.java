package net.kunmc.lab.commandlib.nms.spigot_1_18_1;

import net.kunmc.lab.commandlib.util.nms.world.NMSVec2D;
import net.minecraft.world.phys.Vec2;

public class NMSVec2D_spigot_1_18_1 extends NMSVec2D {
    public NMSVec2D_spigot_1_18_1(Object handle) {
        super(handle, "world.phys.Vec2F");
    }

    @Override
    public float x() {
        return ((Vec2) getHandle()).x;
    }

    @Override
    public float y() {
        return ((Vec2) getHandle()).y;
    }
}
