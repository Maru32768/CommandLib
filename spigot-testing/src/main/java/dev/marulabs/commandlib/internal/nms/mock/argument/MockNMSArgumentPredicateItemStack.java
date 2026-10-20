package dev.marulabs.commandlib.internal.nms.mock.argument;

import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentPredicateItemStack;
import dev.marulabs.commandlib.internal.nms.mock.world.MockNMSItemStack;
import dev.marulabs.commandlib.internal.nms.world.NMSItemStack;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class MockNMSArgumentPredicateItemStack extends NMSArgumentPredicateItemStack {
    private final String materialName;

    public MockNMSArgumentPredicateItemStack(String materialName) {
        super(null, Object.class);
        this.materialName = materialName;
    }

    @Override
    public NMSItemStack createItemStack(int amount, boolean checkOverStack) {
        return new MockNMSItemStack(materialName);
    }
}
