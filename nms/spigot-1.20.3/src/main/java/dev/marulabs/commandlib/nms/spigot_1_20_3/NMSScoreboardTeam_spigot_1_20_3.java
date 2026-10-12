package dev.marulabs.commandlib.nms.spigot_1_20_3;

import dev.marulabs.commandlib.util.nms.world.NMSScoreboardTeam;
import net.minecraft.world.scores.PlayerTeam;

public class NMSScoreboardTeam_spigot_1_20_3 extends NMSScoreboardTeam {
    public NMSScoreboardTeam_spigot_1_20_3(Object handle) {
        super(handle, PlayerTeam.class);
    }

    @Override
    public String getName() {
        return ((PlayerTeam) getHandle()).getName();
    }
}
