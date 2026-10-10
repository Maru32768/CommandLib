package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import net.kunmc.lab.commandlib.util.nms.world.NMSVec2D;
import net.minecraft.server.v1_16_R3.Vec2F;

public class NMSVec2D_spigot_1_16_5 extends NMSVec2D {
    public NMSVec2D_spigot_1_16_5(Object handle) {
        super(handle, "Vec2F");
    }

    @Override
    public float x() {
        return ((Vec2F) getHandle()).i;
    }

    @Override
    public float y() {
        return ((Vec2F) getHandle()).j;
    }
}
