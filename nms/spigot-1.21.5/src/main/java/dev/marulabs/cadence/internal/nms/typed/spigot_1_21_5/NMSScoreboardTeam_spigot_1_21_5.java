package dev.marulabs.cadence.internal.nms.typed.spigot_1_21_5;

import dev.marulabs.cadence.internal.nms.world.NMSScoreboardTeam;
import net.minecraft.world.scores.PlayerTeam;

public class NMSScoreboardTeam_spigot_1_21_5 extends NMSScoreboardTeam {
    public NMSScoreboardTeam_spigot_1_21_5(Object handle) {
        super(handle, PlayerTeam.class);
    }

    @Override
    public String getName() {
        return ((PlayerTeam) requireHandle()).getName();
    }
}
