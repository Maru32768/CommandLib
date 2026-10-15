package dev.marulabs.sampleproxy;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.PlayerArgument;
import dev.marulabs.commandlib.argument.ServerArgument;

/**
 * /send <player> <server>
 */
public class SendCommand extends Command {
    public SendCommand() {
        super("send");

        // 'player' is a com.velocitypowered.api.proxy.Player, 'server' a RegisteredServer.
        argument(new PlayerArgument("player"), new ServerArgument("server")).execute((player, server, ctx) -> {
            player.createConnectionRequest(server)
                  .fireAndForget();
            ctx.sendSuccess("Sending " + player.getUsername() + " to " + server.getServerInfo()
                                                                               .getName());
        });
    }
}
