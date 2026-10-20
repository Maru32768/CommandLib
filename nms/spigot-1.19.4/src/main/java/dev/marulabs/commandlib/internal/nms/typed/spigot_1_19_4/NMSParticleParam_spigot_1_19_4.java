package dev.marulabs.commandlib.internal.nms.typed.spigot_1_19_4;

import dev.marulabs.commandlib.internal.nms.core.NMSParticle;
import dev.marulabs.commandlib.internal.nms.core.NMSParticleParam;
import net.minecraft.core.particles.ParticleOptions;

public class NMSParticleParam_spigot_1_19_4 extends NMSParticleParam {
    public NMSParticleParam_spigot_1_19_4(Object handle) {
        super(handle, ParticleOptions.class);
    }

    @Override
    public NMSParticle getParticle() {
        return NMSParticle.create(((ParticleOptions) requireHandle()).getType());
    }
}
