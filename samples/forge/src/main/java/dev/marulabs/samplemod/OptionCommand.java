package dev.marulabs.samplemod;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.CommandOption;
import dev.marulabs.commandlib.Options;
import dev.marulabs.commandlib.argument.StringArgument;

/**
 * /broadcast [-s|--silent] [-r|--repeat <count>] <message>
 */
public class OptionCommand extends Command {
    public OptionCommand() {
        super("broadcast");

        CommandOption<Boolean, CommandContext> silent = option(Options.flag("silent", 's')
                                                                      .description("Do not send the broadcast"));
        CommandOption<Integer, CommandContext> repeat = option(Options.integer("repeat", 'r', 1, 1, 5)
                                                                      .description("Number of times to send"));

        argument(new StringArgument("message", StringArgument.Type.PHRASE)).execute((ctx, message) -> {
            if (ctx.getOption(silent)) {
                ctx.sendWarn("Broadcast skipped: " + message);
                return;
            }
            for (int i = 0; i < ctx.getOption(repeat); i++) {
                ctx.sendSuccess("[Broadcast] " + message);
            }
        });
    }
}
