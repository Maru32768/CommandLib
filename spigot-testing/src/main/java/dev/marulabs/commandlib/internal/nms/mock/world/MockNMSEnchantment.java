package dev.marulabs.commandlib.internal.nms.mock.world;

import dev.marulabs.commandlib.internal.nms.world.NMSEnchantment;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
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
