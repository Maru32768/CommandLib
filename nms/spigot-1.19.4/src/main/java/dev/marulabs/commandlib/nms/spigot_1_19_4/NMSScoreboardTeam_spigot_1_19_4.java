package dev.marulabs.commandlib.nms.spigot_1_19_4;

import dev.marulabs.commandlib.util.nms.world.NMSScoreboardTeam;
import net.minecraft.world.scores.PlayerTeam;

public class NMSScoreboardTeam_spigot_1_19_4 extends NMSScoreboardTeam {
    public NMSScoreboardTeam_spigot_1_19_4(Object handle) {
        super(handle, PlayerTeam.class);
    }

    @Override
    public String getName() {
        return ((PlayerTeam) getHandle()).getName();
    }
}
