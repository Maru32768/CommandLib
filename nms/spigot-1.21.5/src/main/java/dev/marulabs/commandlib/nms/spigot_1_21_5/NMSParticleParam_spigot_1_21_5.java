package dev.marulabs.commandlib.nms.spigot_1_21_5;

import dev.marulabs.commandlib.util.nms.core.NMSParticle;
import dev.marulabs.commandlib.util.nms.core.NMSParticleParam;
import net.minecraft.core.particles.ParticleOptions;

public class NMSParticleParam_spigot_1_21_5 extends NMSParticleParam {
    public NMSParticleParam_spigot_1_21_5(Object handle) {
        super(handle, ParticleOptions.class);
    }

    @Override
    public NMSParticle getParticle() {
        return NMSParticle.create(((ParticleOptions) getHandle()).getType());
    }
}
