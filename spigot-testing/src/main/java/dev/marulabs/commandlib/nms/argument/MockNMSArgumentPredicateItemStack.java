package dev.marulabs.commandlib.nms.argument;

import dev.marulabs.commandlib.nms.world.MockNMSItemStack;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentPredicateItemStack;
import dev.marulabs.commandlib.util.nms.world.NMSItemStack;

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
