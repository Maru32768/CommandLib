package dev.marulabs.cadence.internal.nms.typed.spigot_1_21_11;

import dev.marulabs.cadence.internal.nms.core.NMSParticle;
import dev.marulabs.cadence.internal.nms.core.NMSParticleParam;
import net.minecraft.core.particles.ParticleOptions;

public class NMSParticleParam_spigot_1_21_11 extends NMSParticleParam {
    public NMSParticleParam_spigot_1_21_11(Object handle) {
        super(handle, ParticleOptions.class);
    }

    @Override
    public NMSParticle getParticle() {
        return NMSParticle.create(((ParticleOptions) requireHandle()).getType());
    }
}
