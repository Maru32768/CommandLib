package dev.marulabs.commandlib.internal.nms.typed.spigot_1_21_11;

import dev.marulabs.commandlib.internal.nms.core.NMSParticleParam;
import dev.marulabs.commandlib.internal.nms.resources.NMSCraftParticle;
import net.minecraft.core.particles.ParticleOptions;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_21_R7.CraftParticle;

public class NMSCraftParticle_spigot_1_21_11 extends NMSCraftParticle {
    public NMSCraftParticle_spigot_1_21_11() {
        super(null, CraftParticle.class);
    }

    @Override
    public Particle toBukkit(NMSParticleParam nms) {
        return CraftParticle.minecraftToBukkit(((ParticleOptions) nms.requireHandle()).getType());
    }
}
