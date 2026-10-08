package net.kunmc.lab.commandlib.nms.spigot_1_19;

import net.kunmc.lab.commandlib.util.nms.world.NMSVec3D;
import net.minecraft.world.phys.Vec3;

public class NMSVec3D_spigot_1_19 extends NMSVec3D {
    public NMSVec3D_spigot_1_19(Object handle) {
        super(handle, "world.phys.Vec3D");
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
