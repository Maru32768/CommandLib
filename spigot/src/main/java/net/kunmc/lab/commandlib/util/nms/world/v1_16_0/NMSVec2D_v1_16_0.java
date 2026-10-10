package net.kunmc.lab.commandlib.util.nms.world.v1_16_0;

import net.kunmc.lab.commandlib.util.nms.world.NMSVec2D;

public class NMSVec2D_v1_16_0 extends NMSVec2D {
    public NMSVec2D_v1_16_0(Object handle) {
        super(handle, "Vec2F");
    }

    @Override
    public float x() {
        return getValue(Float.class, "i");
    }

    @Override
    public float y() {
        return getValue(Float.class, "j");
    }
}
