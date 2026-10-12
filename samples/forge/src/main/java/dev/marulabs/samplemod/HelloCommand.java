package dev.marulabs.samplemod;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.PlayersArgument;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;

/**
 * By registering this, you can use the command below.
 * /hello <players>
 */
public class HelloCommand extends Command {
    public HelloCommand() {
        super("hello");

        argument(new PlayersArgument("players")).execute((players, ctx) -> {
            players.forEach(x -> {
                x.sendMessage(new StringTextComponent("Hello " + x.getName()
                                                                  .getString()
                        + "!"), Util.DUMMY_UUID);
            });
        });
    }
}
