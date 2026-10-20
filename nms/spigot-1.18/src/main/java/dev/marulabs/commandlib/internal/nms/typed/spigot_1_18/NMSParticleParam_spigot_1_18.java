package dev.marulabs.commandlib.internal.nms.typed.spigot_1_18;

import dev.marulabs.commandlib.internal.nms.core.NMSParticle;
import dev.marulabs.commandlib.internal.nms.core.NMSParticleParam;
import net.minecraft.core.particles.ParticleOptions;

public class NMSParticleParam_spigot_1_18 extends NMSParticleParam {
    public NMSParticleParam_spigot_1_18(Object handle) {
        super(handle, ParticleOptions.class);
    }

    @Override
    public NMSParticle getParticle() {
        return NMSParticle.create(((ParticleOptions) requireHandle()).getType());
    }
}
