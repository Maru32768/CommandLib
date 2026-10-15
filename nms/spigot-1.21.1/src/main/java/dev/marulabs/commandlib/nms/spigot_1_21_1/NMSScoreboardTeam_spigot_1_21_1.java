package dev.marulabs.commandlib.nms.spigot_1_21_1;

import dev.marulabs.commandlib.util.nms.world.NMSScoreboardTeam;
import net.minecraft.world.scores.PlayerTeam;

public class NMSScoreboardTeam_spigot_1_21_1 extends NMSScoreboardTeam {
    public NMSScoreboardTeam_spigot_1_21_1(Object handle) {
        super(handle, PlayerTeam.class);
    }

    @Override
    public String getName() {
        return ((PlayerTeam) requireHandle()).getName();
    }
}
