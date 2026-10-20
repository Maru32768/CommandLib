package dev.marulabs.commandlib.internal.nms.typed.spigot_1_16_5;

import dev.marulabs.commandlib.internal.nms.world.NMSScoreboardTeam;
import net.minecraft.server.v1_16_R3.ScoreboardTeam;

public class NMSScoreboardTeam_spigot_1_16_5 extends NMSScoreboardTeam {
    public NMSScoreboardTeam_spigot_1_16_5(Object handle) {
        super(handle, ScoreboardTeam.class);
    }

    @Override
    public String getName() {
        return ((ScoreboardTeam) requireHandle()).getName();
    }
}
