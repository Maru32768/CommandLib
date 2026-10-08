package net.kunmc.lab.commandlib.nms.spigot_1_18;

import net.kunmc.lab.commandlib.util.nms.world.NMSScoreboardTeam;
import net.minecraft.world.scores.PlayerTeam;

public class NMSScoreboardTeam_spigot_1_18 extends NMSScoreboardTeam {
    public NMSScoreboardTeam_spigot_1_18(Object handle) {
        super(handle, "world.scores.ScoreboardTeam");
    }

    @Override
    public String getName() {
        return ((PlayerTeam) getHandle()).getName();
    }
}
