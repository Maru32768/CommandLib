package net.kunmc.lab.commandlib.nms.world;

import net.kunmc.lab.commandlib.util.nms.world.NMSItemStack;

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
