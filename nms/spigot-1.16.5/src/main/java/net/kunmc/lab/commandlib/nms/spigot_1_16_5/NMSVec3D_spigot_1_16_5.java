package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import net.kunmc.lab.commandlib.util.nms.world.NMSVec3D;
import net.minecraft.server.v1_16_R3.Vec3D;

public class NMSVec3D_spigot_1_16_5 extends NMSVec3D {
    public NMSVec3D_spigot_1_16_5(Object handle) {
        super(handle, "Vec3D");
    }

    @Override
    public double x() {
        return ((Vec3D) getHandle()).x;
    }

    @Override
    public double y() {
        return ((Vec3D) getHandle()).y;
    }

    @Override
    public double z() {
        return ((Vec3D) getHandle()).z;
    }
}
