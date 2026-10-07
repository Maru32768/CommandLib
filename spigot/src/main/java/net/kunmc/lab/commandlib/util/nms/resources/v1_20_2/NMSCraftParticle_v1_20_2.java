package net.kunmc.lab.commandlib.util.nms.resources.v1_20_2;

import net.kunmc.lab.commandlib.util.nms.core.NMSParticle;
import net.kunmc.lab.commandlib.util.nms.core.NMSParticleParam;
import net.kunmc.lab.commandlib.util.nms.resources.NMSCraftParticle;
import org.bukkit.Particle;

public class NMSCraftParticle_v1_20_2 extends NMSCraftParticle {
    public NMSCraftParticle_v1_20_2() {
        super(null, "CraftParticle");
    }

    public Particle toBukkit(NMSParticleParam nms) {
        // minecraftToBukkit takes the particle type. Only simple particles are their own options, so the options are
        // converted to their type first.
        NMSParticle particle = nms.getParticle();
        return ((Particle) invokeStaticMethod(new String[]{"minecraftToBukkit"},
                                              new Class<?>[]{particle.getFoundClass()},
                                              particle.getHandle()));
    }
}
