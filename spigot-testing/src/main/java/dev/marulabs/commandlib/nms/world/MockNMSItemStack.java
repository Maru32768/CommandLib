package dev.marulabs.commandlib.nms.world;

import dev.marulabs.commandlib.util.nms.world.NMSItemStack;

public class MockNMSItemStack extends NMSItemStack {
    private final String materialName;

    public MockNMSItemStack(String materialName) {
        super(null, Object.class);
        this.materialName = materialName;
    }

    public String getMaterialName() {
        return materialName;
    }
}
