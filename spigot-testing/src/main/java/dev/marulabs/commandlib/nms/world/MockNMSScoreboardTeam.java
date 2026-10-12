package dev.marulabs.commandlib.nms.world;

import dev.marulabs.commandlib.util.nms.world.NMSScoreboardTeam;

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
