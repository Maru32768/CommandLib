package dev.marulabs.commandlib.nms.spigot_26_1_2;

import dev.marulabs.commandlib.util.nms.core.NMSParticleParam;
import dev.marulabs.commandlib.util.nms.resources.NMSCraftParticle;
import net.minecraft.core.particles.ParticleOptions;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.CraftParticle;

public class NMSCraftParticle_spigot_26_1_2 extends NMSCraftParticle {
    public NMSCraftParticle_spigot_26_1_2() {
        super(null, CraftParticle.class);
    }

    @Override
    public Particle toBukkit(NMSParticleParam nms) {
        return CraftParticle.minecraftToBukkit(((ParticleOptions) nms.getHandle()).getType());
    }
}
