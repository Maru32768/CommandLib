package dev.marulabs.cadence.internal.nms.mock.world;

import dev.marulabs.cadence.internal.nms.world.NMSScoreboardTeam;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
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
