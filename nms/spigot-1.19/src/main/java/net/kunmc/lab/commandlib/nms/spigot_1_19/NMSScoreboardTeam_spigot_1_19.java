package net.kunmc.lab.commandlib.nms.spigot_1_19;

import net.kunmc.lab.commandlib.util.nms.world.NMSScoreboardTeam;
import net.minecraft.world.scores.PlayerTeam;

public class NMSScoreboardTeam_spigot_1_19 extends NMSScoreboardTeam {
    public NMSScoreboardTeam_spigot_1_19(Object handle) {
        super(handle, "world.scores.ScoreboardTeam");
    }

    @Override
    public String getName() {
        return ((PlayerTeam) getHandle()).getName();
    }
}
