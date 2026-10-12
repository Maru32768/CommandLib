package dev.marulabs.commandlib.nms.spigot_1_19_3;

import dev.marulabs.commandlib.util.nms.world.NMSVec2D;
import net.minecraft.world.phys.Vec2;

public class NMSVec2D_spigot_1_19_3 extends NMSVec2D {
    public NMSVec2D_spigot_1_19_3(Object handle) {
        super(handle, Vec2.class);
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
