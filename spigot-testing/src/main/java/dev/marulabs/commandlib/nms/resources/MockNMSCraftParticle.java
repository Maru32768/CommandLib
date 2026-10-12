package dev.marulabs.commandlib.nms.resources;

import dev.marulabs.commandlib.nms.argument.MockArgumentTypes;
import dev.marulabs.commandlib.nms.core.MockNMSParticleParam;
import dev.marulabs.commandlib.util.nms.core.NMSParticleParam;
import dev.marulabs.commandlib.util.nms.resources.NMSCraftParticle;
import org.bukkit.Particle;

public class MockNMSCraftParticle extends NMSCraftParticle {
    public MockNMSCraftParticle() {
        super(null, Object.class);
    }

    @Override
    public Particle toBukkit(NMSParticleParam nms) {
        return Particle.valueOf(MockArgumentTypes.bukkitName(((MockNMSParticleParam) nms).getMockName()));
    }
}
