package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import net.kunmc.lab.commandlib.util.nms.world.NMSScoreboardTeam;
import net.minecraft.server.v1_16_R3.ScoreboardTeam;

public class NMSScoreboardTeam_spigot_1_16_5 extends NMSScoreboardTeam {
    public NMSScoreboardTeam_spigot_1_16_5(Object handle) {
        super(handle, "ScoreboardTeam");
    }

    @Override
    public String getName() {
        return ((ScoreboardTeam) getHandle()).getName();
    }
}
