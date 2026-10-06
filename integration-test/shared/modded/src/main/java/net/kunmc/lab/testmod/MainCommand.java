package net.kunmc.lab.testmod;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.DefaultPermission;

public final class MainCommand extends Command {
    public static final String NAME = "commandlibtest";

    public MainCommand() {
        super(NAME);
        permission(DefaultPermission.ALL);
    }
}
