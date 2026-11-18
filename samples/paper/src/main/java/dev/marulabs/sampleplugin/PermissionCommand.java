package dev.marulabs.sampleplugin;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.DefaultPermission;

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
