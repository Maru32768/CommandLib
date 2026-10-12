package dev.marulabs.commandlib.nms.argument;

import dev.marulabs.commandlib.nms.world.MockNMSIBlockData;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentTileLocation;
import dev.marulabs.commandlib.util.nms.world.NMSIBlockData;

public class MockNMSArgumentTileLocation extends NMSArgumentTileLocation {
    private final String materialName;

    public MockNMSArgumentTileLocation(String materialName) {
        super(null, Object.class);
        this.materialName = materialName;
    }

    @Override
    public NMSIBlockData getBlockData() {
        return new MockNMSIBlockData(materialName);
    }
}
