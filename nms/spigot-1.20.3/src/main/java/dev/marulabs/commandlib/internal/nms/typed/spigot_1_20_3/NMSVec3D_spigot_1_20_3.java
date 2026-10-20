package dev.marulabs.commandlib.internal.nms.typed.spigot_1_20_3;

import dev.marulabs.commandlib.internal.nms.world.NMSVec3D;
import net.minecraft.world.phys.Vec3;

public class NMSVec3D_spigot_1_20_3 extends NMSVec3D {
    public NMSVec3D_spigot_1_20_3(Object handle) {
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
