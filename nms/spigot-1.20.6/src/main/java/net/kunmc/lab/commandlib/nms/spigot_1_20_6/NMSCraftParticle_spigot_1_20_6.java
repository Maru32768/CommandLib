package net.kunmc.lab.commandlib.nms.spigot_1_20_6;

import net.kunmc.lab.commandlib.util.nms.core.NMSParticleParam;
import net.kunmc.lab.commandlib.util.nms.resources.NMSCraftParticle;
import net.minecraft.core.particles.ParticleOptions;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_20_R4.CraftParticle;

public class NMSCraftParticle_spigot_1_20_6 extends NMSCraftParticle {
    public NMSCraftParticle_spigot_1_20_6() {
        super(null, "CraftParticle");
    }

    @Override
    public Particle toBukkit(NMSParticleParam nms) {
        return CraftParticle.minecraftToBukkit(((ParticleOptions) nms.getHandle()).getType());
    }
}
