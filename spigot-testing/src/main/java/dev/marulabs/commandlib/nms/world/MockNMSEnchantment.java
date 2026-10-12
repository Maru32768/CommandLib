package dev.marulabs.commandlib.nms.world;

import dev.marulabs.commandlib.util.nms.world.NMSEnchantment;

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
