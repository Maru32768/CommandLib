package dev.marulabs.commandlib.nms.core;

import dev.marulabs.commandlib.util.nms.core.NMSParticle;
import dev.marulabs.commandlib.util.nms.core.NMSParticleParam;

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
