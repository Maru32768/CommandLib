package dev.marulabs.commandlib.nms.world;

import dev.marulabs.commandlib.util.nms.world.NMSVec3D;

public class MockNMSVec3D extends NMSVec3D {
    private final double x;
    private final double y;
    private final double z;

    public MockNMSVec3D(double x, double y, double z) {
        super(null, Object.class);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public double x() {
        return x;
    }

    @Override
    public double y() {
        return y;
    }

    @Override
    public double z() {
        return z;
    }
}
