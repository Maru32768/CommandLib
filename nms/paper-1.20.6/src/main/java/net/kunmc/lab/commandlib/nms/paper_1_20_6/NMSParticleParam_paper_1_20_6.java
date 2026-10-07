package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import net.kunmc.lab.commandlib.util.nms.core.NMSParticle;
import net.kunmc.lab.commandlib.util.nms.core.NMSParticleParam;
import net.minecraft.core.particles.ParticleOptions;

public class NMSParticleParam_paper_1_20_6 extends NMSParticleParam {
    public NMSParticleParam_paper_1_20_6(Object handle) {
        super(handle, "core.particles.ParticleOptions");
    }

    @Override
    public NMSParticle getParticle() {
        return NMSParticle.create(((ParticleOptions) getHandle()).getType());
    }
}
