package net.kunmc.lab.commandlib.nms.spigot_1_17;

import net.kunmc.lab.commandlib.util.nms.core.NMSParticle;
import net.kunmc.lab.commandlib.util.nms.core.NMSParticleParam;
import net.minecraft.core.particles.ParticleOptions;

public class NMSParticleParam_spigot_1_17 extends NMSParticleParam {
    public NMSParticleParam_spigot_1_17(Object handle) {
        super(handle, ParticleOptions.class);
    }

    @Override
    public NMSParticle getParticle() {
        return NMSParticle.create(((ParticleOptions) getHandle()).getType());
    }
}
