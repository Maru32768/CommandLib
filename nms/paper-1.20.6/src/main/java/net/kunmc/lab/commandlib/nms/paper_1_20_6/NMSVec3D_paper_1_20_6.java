package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import net.kunmc.lab.commandlib.util.nms.world.NMSVec3D;
import net.minecraft.world.phys.Vec3;

public class NMSVec3D_paper_1_20_6 extends NMSVec3D {
    public NMSVec3D_paper_1_20_6(Object handle) {
        super(handle, "world.phys.Vec3");
    }

    @Override
    public double x() {
        return ((Vec3) getHandle()).x;
    }

    @Override
    public double y() {
        return ((Vec3) getHandle()).y;
    }

    @Override
    public double z() {
        return ((Vec3) getHandle()).z;
    }
}
