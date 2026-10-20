package dev.marulabs.commandlib.internal.nms.mock.core;

import dev.marulabs.commandlib.internal.nms.core.NMSParticle;
import dev.marulabs.commandlib.internal.nms.core.NMSParticleParam;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class MockNMSParticleParam extends NMSParticleParam {
    private final String mockName;

    public MockNMSParticleParam(String mockName) {
        super(null, Object.class);
        this.mockName = mockName;
    }

    public String getMockName() {
        return mockName;
    }

    @Override
    public NMSParticle getParticle() {
        throw new UnsupportedOperationException("getParticle() is not supported in MockNMSParticleParam");
    }
}
