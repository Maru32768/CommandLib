package net.kunmc.lab.commandlib.nms.argument;

import net.kunmc.lab.commandlib.nms.world.MockNMSItemStack;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentPredicateItemStack;
import net.kunmc.lab.commandlib.util.nms.world.NMSItemStack;

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
