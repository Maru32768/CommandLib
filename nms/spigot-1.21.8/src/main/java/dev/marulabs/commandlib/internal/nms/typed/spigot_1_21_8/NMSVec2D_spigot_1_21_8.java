package dev.marulabs.commandlib.internal.nms.typed.spigot_1_21_8;

import dev.marulabs.commandlib.internal.nms.world.NMSVec2D;
import net.minecraft.world.phys.Vec2;

public class NMSVec2D_spigot_1_21_8 extends NMSVec2D {
    public NMSVec2D_spigot_1_21_8(Object handle) {
        super(handle, Vec2.class);
    }

    @Override
    public float x() {
        return ((Vec2) requireHandle()).x;
    }

    @Override
    public float y() {
        return ((Vec2) requireHandle()).y;
    }
}
