package dev.marulabs.samplemod;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.CommandOption;
import dev.marulabs.cadence.Options;
import dev.marulabs.cadence.argument.StringArgument;

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
