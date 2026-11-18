package dev.marulabs.cadence.examples.ban;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.CommandOption;
import dev.marulabs.cadence.DefaultPermission;
import dev.marulabs.cadence.Options;
import dev.marulabs.cadence.argument.PlayerArgument;

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
