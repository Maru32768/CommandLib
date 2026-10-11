package net.kunmc.lab.commandlib.nms.world;

import net.kunmc.lab.commandlib.util.nms.world.NMSEnchantment;

public class MockNMSEnchantment extends NMSEnchantment {
    private final String mockName;

    public MockNMSEnchantment(String mockName) {
        super(null, Object.class);
        this.mockName = mockName;
    }

    public String getMockName() {
        return mockName;
    }
}
