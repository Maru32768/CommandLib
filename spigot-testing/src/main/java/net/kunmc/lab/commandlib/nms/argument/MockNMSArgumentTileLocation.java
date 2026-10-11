package net.kunmc.lab.commandlib.nms.argument;

import net.kunmc.lab.commandlib.nms.world.MockNMSIBlockData;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentTileLocation;
import net.kunmc.lab.commandlib.util.nms.world.NMSIBlockData;

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
