package net.kunmc.lab.commandlib.nms.world;

import net.kunmc.lab.commandlib.util.nms.world.NMSScoreboardTeam;

public class MockNMSScoreboardTeam extends NMSScoreboardTeam {
    private final String name;

    public MockNMSScoreboardTeam(String name) {
        super(null, Object.class);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
