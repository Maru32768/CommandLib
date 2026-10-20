package dev.marulabs.commandlib.internal.nms.typed.spigot_1_19_3;

import dev.marulabs.commandlib.internal.nms.core.NMSParticleParam;
import dev.marulabs.commandlib.internal.nms.resources.NMSCraftParticle;
import net.minecraft.core.particles.ParticleOptions;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_19_R2.CraftParticle;

public class NMSCraftParticle_spigot_1_19_3 extends NMSCraftParticle {
    public NMSCraftParticle_spigot_1_19_3() {
        super(null, CraftParticle.class);
    }

    @Override
    public Particle toBukkit(NMSParticleParam nms) {
        return CraftParticle.toBukkit((ParticleOptions) nms.requireHandle());
    }
}
