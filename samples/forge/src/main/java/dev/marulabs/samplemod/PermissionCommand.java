package dev.marulabs.samplemod;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.DefaultPermission;

/**
 * /admincheck
 */
public class PermissionCommand extends Command {
    public PermissionCommand() {
        super("admincheck");

        permission("sample.command.admincheck", DefaultPermission.OP, "Run the sample admin command");
        execute(ctx -> {
            ctx.sendSuccess("You can run admin commands.");
        });
    }
}
