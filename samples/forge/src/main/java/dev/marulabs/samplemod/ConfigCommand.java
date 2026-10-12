package dev.marulabs.samplemod;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.IntegerArgument;

/**
 * By registering this, you can use the command below.
 * /config get
 * /config set <integer>
 */
public class ConfigCommand extends Command {
    private int intValue = 0;

    public ConfigCommand() {
        super("config");

        Command getCommand = new Command("get");
        getCommand.execute(ctx -> {
            ctx.sendSuccess("Current value is " + intValue);
        });

        Command setCommand = new Command("set");
        setCommand.argument(new IntegerArgument("number"))
                  .execute((number, ctx) -> {
                      intValue = number;
                      ctx.sendSuccess("Changed intValue to " + intValue);
                  });

        addChildren(getCommand, setCommand);
    }
}
