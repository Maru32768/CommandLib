package dev.marulabs.commandlib.nms.spigot_1_16_5;

import dev.marulabs.commandlib.util.nms.core.NMSParticleParam;
import dev.marulabs.commandlib.util.nms.resources.NMSCraftParticle;
import net.minecraft.server.v1_16_R3.ParticleParam;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_16_R3.CraftParticle;

public class NMSCraftParticle_spigot_1_16_5 extends NMSCraftParticle {
    public NMSCraftParticle_spigot_1_16_5() {
        super(null, CraftParticle.class);
    }

    @Override
    public Particle toBukkit(NMSParticleParam nms) {
        return CraftParticle.toBukkit((ParticleParam) nms.requireHandle());
    }
}
