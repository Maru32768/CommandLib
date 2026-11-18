package dev.marulabs.cadence.examples.ban;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.CommandOption;
import dev.marulabs.cadence.DefaultPermission;
import dev.marulabs.cadence.Options;
import dev.marulabs.cadence.argument.PlayerArgument;

/**
 * Use case 1, an admin command with options: {@code /ban [-s|--silent] [-r|--reason <reason>] <target>}.
 */
public final class FluentBanCommand extends Command {
    public FluentBanCommand() {
        super("ban");
        description("Ban a player");
        permission(DefaultPermission.OP, "Ban players");

        CommandOption<Boolean, CommandContext> silent = option(Options.flag("silent", 's')
                                                                      .description("Do not announce the ban"));
        CommandOption<String, CommandContext> reason = option(Options.string("reason", 'r', "No reason")
                                                                     .description("Why the player is banned"));
        argument(new PlayerArgument("target")).execute((ctx, target) -> {
            ctx.sendSuccess("Banned " + target.getName() + ": " + ctx.getOption(reason)
                    + (ctx.getOption(silent) ? " (silent)" : ""));
        });
    }
}
