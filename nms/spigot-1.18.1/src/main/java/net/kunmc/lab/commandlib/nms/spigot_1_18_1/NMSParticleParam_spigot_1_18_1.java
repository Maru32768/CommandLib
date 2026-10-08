package net.kunmc.lab.commandlib.nms.spigot_1_18_1;

import net.kunmc.lab.commandlib.util.nms.core.NMSParticle;
import net.kunmc.lab.commandlib.util.nms.core.NMSParticleParam;
import net.minecraft.core.particles.ParticleOptions;

public class NMSParticleParam_spigot_1_18_1 extends NMSParticleParam {
    public NMSParticleParam_spigot_1_18_1(Object handle) {
        super(handle, "core.particles.ParticleParam");
    }

    @Override
    public NMSParticle getParticle() {
        return NMSParticle.create(((ParticleOptions) getHandle()).getType());
    }
}
