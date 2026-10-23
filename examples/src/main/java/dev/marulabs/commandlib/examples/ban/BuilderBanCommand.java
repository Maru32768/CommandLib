package dev.marulabs.commandlib.examples.ban;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.CommandOption;
import dev.marulabs.commandlib.DefaultPermission;
import dev.marulabs.commandlib.Options;
import dev.marulabs.commandlib.argument.PlayerArgument;

/**
 * {@link FluentBanCommand} written with the Java builder.
 */
public final class BuilderBanCommand {
    private BuilderBanCommand() {
    }

    public static Command create() {
        CommandOption<Boolean, CommandContext> silent = Options.<CommandContext>flag("silent", 's')
                                                               .description("Do not announce the ban");
        CommandOption<String, CommandContext> reason = Options.<CommandContext>string("reason", 'r', "No reason")
                                                              .description("Why the player is banned");
        return Command.builder("ban")
                      .description("Ban a player")
                      .permission(DefaultPermission.OP, "Ban players")
                      .option(silent)
                      .option(reason)
                      .argument(new PlayerArgument("target"), branch -> branch.execute((ctx, target) -> {
                          ctx.sendSuccess("Banned " + target.getName() + ": " + ctx.getOption(reason)
                                  + (ctx.getOption(silent) ? " (silent)" : ""));
                      }))
                      .build();
    }
}
