package net.kunmc.lab.commandlib.nms.world;

import net.kunmc.lab.commandlib.util.nms.world.NMSIBlockData;

public class MockNMSIBlockData extends NMSIBlockData {
    private final String materialName;

    public MockNMSIBlockData(String materialName) {
        super(null, Object.class);
        this.materialName = materialName;
    }

    public String getMaterialName() {
        return materialName;
    }
}
