package dev.marulabs.commandlib.examples.ban;

import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.DefaultPermission;
import dev.marulabs.commandlib.annotation.Command;
import dev.marulabs.commandlib.annotation.Default;
import dev.marulabs.commandlib.annotation.Description;
import dev.marulabs.commandlib.annotation.Execute;
import dev.marulabs.commandlib.annotation.Flag;
import dev.marulabs.commandlib.annotation.Option;
import dev.marulabs.commandlib.annotation.Permission;
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
