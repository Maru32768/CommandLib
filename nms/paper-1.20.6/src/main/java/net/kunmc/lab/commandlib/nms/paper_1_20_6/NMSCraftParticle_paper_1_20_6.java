package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import net.kunmc.lab.commandlib.util.nms.core.NMSParticleParam;
import net.kunmc.lab.commandlib.util.nms.resources.NMSCraftParticle;
import net.minecraft.core.particles.ParticleOptions;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.CraftParticle;

public class NMSCraftParticle_paper_1_20_6 extends NMSCraftParticle {
    public NMSCraftParticle_paper_1_20_6() {
        super(null, "CraftParticle");
    }

    @Override
    public Particle toBukkit(NMSParticleParam nms) {
        return CraftParticle.minecraftToBukkit(((ParticleOptions) nms.getHandle()).getType());
    }
}
