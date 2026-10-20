package dev.marulabs.commandlib.internal.nms.typed.spigot_1_16_5;

import dev.marulabs.commandlib.internal.nms.world.NMSVec2D;
import net.minecraft.server.v1_16_R3.Vec2F;

public class NMSVec2D_spigot_1_16_5 extends NMSVec2D {
    public NMSVec2D_spigot_1_16_5(Object handle) {
        super(handle, Vec2F.class);
    }

    @Override
    public float x() {
        return ((Vec2F) requireHandle()).i;
    }

    @Override
    public float y() {
        return ((Vec2F) requireHandle()).j;
    }
}
