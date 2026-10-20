package dev.marulabs.commandlib.internal.nms.mock.world;

import dev.marulabs.commandlib.internal.nms.world.NMSItemStack;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
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
