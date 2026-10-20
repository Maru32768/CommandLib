package dev.marulabs.commandlib.internal.nms.mock.argument;

import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentTileLocation;
import dev.marulabs.commandlib.internal.nms.mock.world.MockNMSIBlockData;
import dev.marulabs.commandlib.internal.nms.world.NMSIBlockData;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
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
