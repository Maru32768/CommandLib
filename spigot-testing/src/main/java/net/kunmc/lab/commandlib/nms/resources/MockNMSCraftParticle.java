package net.kunmc.lab.commandlib.nms.resources;

import net.kunmc.lab.commandlib.nms.argument.MockArgumentTypes;
import net.kunmc.lab.commandlib.nms.core.MockNMSParticleParam;
import net.kunmc.lab.commandlib.util.nms.core.NMSParticleParam;
import net.kunmc.lab.commandlib.util.nms.resources.NMSCraftParticle;
import org.bukkit.Particle;

public class MockNMSCraftParticle extends NMSCraftParticle {
    public MockNMSCraftParticle() {
        super(null, "Mock");
    }

    @Override
    public Particle toBukkit(NMSParticleParam nms) {
        return Particle.valueOf(MockArgumentTypes.bukkitName(((MockNMSParticleParam) nms).getMockName()));
    }
}
