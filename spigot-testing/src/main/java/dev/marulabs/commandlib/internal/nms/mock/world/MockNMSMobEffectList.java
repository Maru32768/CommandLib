package dev.marulabs.commandlib.internal.nms.mock.world;

import dev.marulabs.commandlib.internal.nms.world.NMSMobEffectList;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
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
