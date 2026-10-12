package dev.marulabs.testmod;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.DefaultPermission;

public final class MainCommand extends Command {
    public static final String NAME = "commandlibtest";

    public MainCommand() {
        super(NAME);
        permission(DefaultPermission.ALL);
    }
}
