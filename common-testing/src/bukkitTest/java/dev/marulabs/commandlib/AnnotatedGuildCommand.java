package dev.marulabs.commandlib;

import dev.marulabs.commandlib.annotation.Command;
import dev.marulabs.commandlib.annotation.Description;
import dev.marulabs.commandlib.annotation.Examples;
import dev.marulabs.commandlib.annotation.Execute;
import dev.marulabs.commandlib.annotation.Flag;
import dev.marulabs.commandlib.annotation.Middleware;
import dev.marulabs.commandlib.annotation.MutuallyExclusiveOptions;
import dev.marulabs.commandlib.annotation.Option;
import dev.marulabs.commandlib.annotation.Optional;
import dev.marulabs.commandlib.annotation.OptionsAnywhere;
import dev.marulabs.commandlib.annotation.Permission;
import dev.marulabs.commandlib.annotation.Requires;
import dev.marulabs.commandlib.annotation.Subcommand;
import dev.marulabs.commandlib.annotation.Variadic;
import dev.marulabs.commandlib.annotation.VisibleWhen;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The annotations that {@link AnnotatedArenaCommand} does not use; {@link FluentGuildCommand} is the same command
 * written fluently. Parse it with {@link FluentGuildCommand#AUDIT} registered as "audit" and
 * {@link FluentGuildCommand#PLAYERS} as "players".
 */
@Command("guild")
@Description(value = "Manage guilds", caption = "test.guild.description")
@Examples({"/guild red invite Steve", "/guild red kick -s Steve"})
@Middleware("audit")
public final class AnnotatedGuildCommand {
    @Execute
    public void list(CommandContext ctx) {
        ctx.sendSuccess("Guilds: red");
    }

    @Subcommand("<guild> invite")
    @Description("Invite players")
    public void invite(CommandContext ctx, String guild, @Variadic(max = 3) List<Player> players) {
        ctx.sendSuccess("Invited " + players.stream()
                                            .map(Player::getName)
                                            .collect(Collectors.joining(", "))
                + " to " + guild);
    }

    @Subcommand("<guild> kick")
    @OptionsAnywhere
    @MutuallyExclusiveOptions({"silent", "loud"})
    @VisibleWhen("players")
    public void kick(CommandContext ctx,
                     @Flag(value = "silent", shortName = 's') boolean silent,
                     @Flag(value = "loud", shortName = 'l') @Permission(defaultPermission = DefaultPermission.ALL) boolean loud,
                     @Option(value = "reason", shortName = 'r') @Requires("by") String reason,
                     @Option(value = "by", shortName = 'b') @Optional Player by,
                     String guild,
                     Player target) {
        ctx.sendSuccess("Kicked " + target.getName() + " from " + guild + " by " + by.getName() + " for '" + reason
                + "'" + (silent ? " silently" : "") + (loud ? " loudly" : ""));
    }
}
