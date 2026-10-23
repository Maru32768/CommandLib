package dev.marulabs.sampleproxy;

import com.velocitypowered.api.proxy.Player;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.PlayerArgument;

/**
 * /find <players...>
 */
public class FindCommand extends Command {
    public FindCommand() {
        super("find");

        argument(new PlayerArgument("players").variadic()).execute((ctx, players) -> {
            for (Player player : players) {
                String server = player.getCurrentServer()
                                      .map(x -> x.getServerInfo()
                                                 .getName())
                                      .orElse("no server");
                ctx.sendSuccess(player.getUsername() + " is on " + server);
            }
        });
    }
}
