package net.kunmc.lab.commandlib.nms.world;

import net.kunmc.lab.commandlib.util.nms.world.NMSMobEffectList;

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
