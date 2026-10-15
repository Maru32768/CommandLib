package dev.marulabs.samplefabric;

import dev.marulabs.commandlib.Command;

/**
 * /ping
 */
public class PingCommand extends Command {
    public PingCommand() {
        super("ping");

        execute(ctx -> {
            ctx.sendSuccess("pong");
        });
    }
}
