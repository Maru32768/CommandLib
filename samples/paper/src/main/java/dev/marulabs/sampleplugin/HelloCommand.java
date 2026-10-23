package dev.marulabs.sampleplugin;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.PlayersArgument;

/**
 * By registering this, you can use the command below.
 * /hello <players>
 */
public class HelloCommand extends Command {
    public HelloCommand() {
        super("hello");

        argument(new PlayersArgument("players")).execute((ctx, players) -> {
            players.forEach(x -> x.sendMessage("Hello " + x.getName() + "!"));
        });
    }
}
