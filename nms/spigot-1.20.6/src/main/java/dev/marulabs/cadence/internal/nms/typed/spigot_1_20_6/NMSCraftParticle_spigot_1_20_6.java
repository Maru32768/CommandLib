package dev.marulabs.cadence.internal.nms.typed.spigot_1_20_6;

import dev.marulabs.cadence.internal.nms.core.NMSParticleParam;
import dev.marulabs.cadence.internal.nms.resources.NMSCraftParticle;
import net.minecraft.core.particles.ParticleOptions;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_20_R4.CraftParticle;

public class NMSCraftParticle_spigot_1_20_6 extends NMSCraftParticle {
    public NMSCraftParticle_spigot_1_20_6() {
        super(null, CraftParticle.class);
    }

    @Override
    public Particle toBukkit(NMSParticleParam nms) {
        return CraftParticle.minecraftToBukkit(((ParticleOptions) nms.requireHandle()).getType());
    }
}
