package dev.marulabs.commandlib.internal.nms.mock.world;

import dev.marulabs.commandlib.internal.nms.world.NMSIBlockData;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
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
