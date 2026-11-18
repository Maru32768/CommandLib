package dev.marulabs.sampleproxy;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.argument.PlayerArgument;
import dev.marulabs.cadence.argument.ServerArgument;

/**
 * /send <player> <server>
 */
public class SendCommand extends Command {
    public SendCommand() {
        super("send");

        // 'player' is a com.velocitypowered.api.proxy.Player, 'server' a RegisteredServer.
        argument(new PlayerArgument("player"), new ServerArgument("server")).execute((ctx, player, server) -> {
            player.createConnectionRequest(server)
                  .fireAndForget();
            ctx.sendSuccess("Sending " + player.getUsername() + " to " + server.getServerInfo()
                                                                               .getName());
        });
    }
}
