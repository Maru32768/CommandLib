package dev.marulabs.testmod;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.DefaultPermission;

public final class MainCommand extends Command {
    public static final String NAME = "cadencetest";

    public MainCommand() {
        super(NAME);
        permission(DefaultPermission.ALL);
    }
}
