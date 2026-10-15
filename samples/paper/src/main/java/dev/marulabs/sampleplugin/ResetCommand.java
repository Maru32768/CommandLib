package dev.marulabs.sampleplugin;

import dev.marulabs.commandlib.Command;

import java.time.Duration;

/**
 * /reset: run it twice within 10 seconds to confirm; a player can reset once a minute.
 */
public class ResetCommand extends Command {
    public ResetCommand() {
        super("reset");

        confirm();
        cooldown(Duration.ofMinutes(1));
        execute(ctx -> ctx.sendSuccess("Reset."));
    }
}
