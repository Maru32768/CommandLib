package dev.marulabs.commandlib.nms.spigot_1_21_10;

import dev.marulabs.commandlib.util.nms.core.NMSParticleParam;
import dev.marulabs.commandlib.util.nms.resources.NMSCraftParticle;
import net.minecraft.core.particles.ParticleOptions;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_21_R6.CraftParticle;

public class NMSCraftParticle_spigot_1_21_10 extends NMSCraftParticle {
    public NMSCraftParticle_spigot_1_21_10() {
        super(null, CraftParticle.class);
    }

    @Override
    public Particle toBukkit(NMSParticleParam nms) {
        return CraftParticle.minecraftToBukkit(((ParticleOptions) nms.requireHandle()).getType());
    }
}
