package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import net.kunmc.lab.commandlib.util.nms.core.NMSParticle;
import net.kunmc.lab.commandlib.util.nms.core.NMSParticleParam;
import net.minecraft.server.v1_16_R3.ParticleParam;

public class NMSParticleParam_spigot_1_16_5 extends NMSParticleParam {
    public NMSParticleParam_spigot_1_16_5(Object handle) {
        super(handle, "ParticleParam");
    }

    @Override
    public NMSParticle getParticle() {
        return NMSParticle.create(((ParticleParam) getHandle()).getParticle());
    }
}
