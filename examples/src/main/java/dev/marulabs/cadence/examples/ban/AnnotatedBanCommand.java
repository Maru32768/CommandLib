package dev.marulabs.cadence.examples.ban;

import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.DefaultPermission;
import dev.marulabs.cadence.annotation.Command;
import dev.marulabs.cadence.annotation.Default;
import dev.marulabs.cadence.annotation.Description;
import dev.marulabs.cadence.annotation.Execute;
import dev.marulabs.cadence.annotation.Flag;
import dev.marulabs.cadence.annotation.Option;
import dev.marulabs.cadence.annotation.Permission;
import org.bukkit.entity.Player;

/**
 * {@link FluentBanCommand} written with annotations; {@code AnnotatedCommands.parse(new AnnotatedBanCommand())}.
 */
@Command("ban")
@Description("Ban a player")
@Permission(defaultPermission = DefaultPermission.OP, description = "Ban players")
public final class AnnotatedBanCommand {
    @Execute
    public void ban(CommandContext ctx,
                    @Flag(value = "silent", shortName = 's') @Description("Do not announce the ban") boolean silent,
                    @Option(value = "reason", shortName = 'r') @Default("No reason") @Description("Why the player is banned") String reason,
                    Player target) {
        ctx.sendSuccess("Banned " + target.getName() + ": " + reason + (silent ? " (silent)" : ""));
    }
}
