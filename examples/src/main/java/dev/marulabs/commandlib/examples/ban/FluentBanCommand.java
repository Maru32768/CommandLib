package dev.marulabs.commandlib.examples.ban;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.CommandOption;
import dev.marulabs.commandlib.DefaultPermission;
import dev.marulabs.commandlib.Options;
import dev.marulabs.commandlib.argument.PlayerArgument;

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
