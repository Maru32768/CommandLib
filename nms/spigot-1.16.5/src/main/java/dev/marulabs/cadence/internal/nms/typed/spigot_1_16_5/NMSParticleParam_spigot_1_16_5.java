package dev.marulabs.cadence.internal.nms.typed.spigot_1_16_5;

import dev.marulabs.cadence.internal.nms.core.NMSParticle;
import dev.marulabs.cadence.internal.nms.core.NMSParticleParam;
import net.minecraft.server.v1_16_R3.ParticleParam;

public class NMSParticleParam_spigot_1_16_5 extends NMSParticleParam {
    public NMSParticleParam_spigot_1_16_5(Object handle) {
        super(handle, ParticleParam.class);
    }

    @Override
    public NMSParticle getParticle() {
        return NMSParticle.create(((ParticleParam) requireHandle()).getParticle());
    }
}
