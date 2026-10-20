package dev.marulabs.commandlib.internal.nms.typed.spigot_26_1_2;

import dev.marulabs.commandlib.internal.nms.world.NMSScoreboardTeam;
import net.minecraft.world.scores.PlayerTeam;

public class NMSScoreboardTeam_spigot_26_1_2 extends NMSScoreboardTeam {
    public NMSScoreboardTeam_spigot_26_1_2(Object handle) {
        super(handle, PlayerTeam.class);
    }

    @Override
    public String getName() {
        return ((PlayerTeam) requireHandle()).getName();
    }
}
