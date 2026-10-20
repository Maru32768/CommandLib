package dev.marulabs.commandlib.internal.nms.typed.paper_1_21_1;

import dev.marulabs.commandlib.internal.nms.core.NMSParticle;
import dev.marulabs.commandlib.internal.nms.core.NMSParticleParam;
import net.minecraft.core.particles.ParticleOptions;

public class NMSParticleParam_paper_1_21_1 extends NMSParticleParam {
    public NMSParticleParam_paper_1_21_1(Object handle) {
        super(handle, ParticleOptions.class);
    }

    @Override
    public NMSParticle getParticle() {
        return NMSParticle.create(((ParticleOptions) requireHandle()).getType());
    }
}
