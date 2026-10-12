package dev.marulabs.commandlib.nms.paper_1_21_1;

import dev.marulabs.commandlib.util.nms.world.NMSVec3D;
import net.minecraft.world.phys.Vec3;

public class NMSVec3D_paper_1_21_1 extends NMSVec3D {
    public NMSVec3D_paper_1_21_1(Object handle) {
        super(handle, Vec3.class);
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
