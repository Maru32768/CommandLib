package dev.marulabs.cadence.internal.nms.typed.spigot_1_18;

import dev.marulabs.cadence.internal.nms.core.NMSParticleParam;
import dev.marulabs.cadence.internal.nms.resources.NMSCraftParticle;
import net.minecraft.core.particles.ParticleOptions;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_18_R1.CraftParticle;

public class NMSCraftParticle_spigot_1_18 extends NMSCraftParticle {
    public NMSCraftParticle_spigot_1_18() {
        super(null, CraftParticle.class);
    }

    @Override
    public Particle toBukkit(NMSParticleParam nms) {
        return CraftParticle.toBukkit((ParticleOptions) nms.requireHandle());
    }
}
