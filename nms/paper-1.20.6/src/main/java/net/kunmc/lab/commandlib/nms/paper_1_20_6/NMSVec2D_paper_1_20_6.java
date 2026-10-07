package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import net.kunmc.lab.commandlib.util.nms.world.NMSVec2D;
import net.minecraft.world.phys.Vec2;

public class NMSVec2D_paper_1_20_6 extends NMSVec2D {
    public NMSVec2D_paper_1_20_6(Object handle) {
        super(handle, "world.phys.Vec2");
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
