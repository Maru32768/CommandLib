package dev.marulabs.commandlib.nms.world;

import dev.marulabs.commandlib.util.nms.world.NMSMobEffectList;

public class MockNMSMobEffectList extends NMSMobEffectList {
    private final String mockName;

    public MockNMSMobEffectList(String mockName) {
        super(null, Object.class);
        this.mockName = mockName;
    }

    public String getMockName() {
        return mockName;
    }
}
