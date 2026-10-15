package dev.marulabs.commandlib.nms.spigot_1_21_8;

import dev.marulabs.commandlib.util.nms.world.NMSVec3D;
import net.minecraft.world.phys.Vec3;

public class NMSVec3D_spigot_1_21_8 extends NMSVec3D {
    public NMSVec3D_spigot_1_21_8(Object handle) {
        super(handle, Vec3.class);
    }

    @Override
    public double x() {
        return ((Vec3) requireHandle()).x;
    }

    @Override
    public double y() {
        return ((Vec3) requireHandle()).y;
    }

    @Override
    public double z() {
        return ((Vec3) requireHandle()).z;
    }
}
