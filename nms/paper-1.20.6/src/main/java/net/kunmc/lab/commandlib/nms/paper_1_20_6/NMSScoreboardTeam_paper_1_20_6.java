package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import net.kunmc.lab.commandlib.util.nms.world.NMSScoreboardTeam;
import net.minecraft.world.scores.PlayerTeam;

public class NMSScoreboardTeam_paper_1_20_6 extends NMSScoreboardTeam {
    public NMSScoreboardTeam_paper_1_20_6(Object handle) {
        super(handle, "world.scores.PlayerTeam");
    }

    @Override
    public String getName() {
        return ((PlayerTeam) getHandle()).getName();
    }
}
