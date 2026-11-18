package dev.marulabs.cadence.internal.nms.mock.resources;

import dev.marulabs.cadence.internal.nms.core.NMSParticleParam;
import dev.marulabs.cadence.internal.nms.mock.argument.MockArgumentTypes;
import dev.marulabs.cadence.internal.nms.mock.core.MockNMSParticleParam;
import dev.marulabs.cadence.internal.nms.resources.NMSCraftParticle;
import org.bukkit.Particle;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class MockNMSCraftParticle extends NMSCraftParticle {
    public MockNMSCraftParticle() {
        super(null, Object.class);
    }

    @Override
    public Particle toBukkit(NMSParticleParam nms) {
        return Particle.valueOf(MockArgumentTypes.bukkitName(((MockNMSParticleParam) nms).getMockName()));
    }
}
