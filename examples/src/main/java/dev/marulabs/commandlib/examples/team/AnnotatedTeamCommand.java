package dev.marulabs.commandlib.examples.team;

import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.annotation.AnnotatedCommands;
import dev.marulabs.commandlib.annotation.Command;
import dev.marulabs.commandlib.annotation.Description;
import dev.marulabs.commandlib.annotation.Execute;
import dev.marulabs.commandlib.annotation.Subcommand;
import dev.marulabs.commandlib.argument.ObjectArgument;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * {@link FluentTeamCommand} with annotations. The {@code <team>} placeholder of the paths is the argument the children
 * follow, made by the factory registered for {@code Set}, and each method takes its value as the {@code team}
 * parameter.
 */
@Command("team")
@Description("Manage team members")
public final class AnnotatedTeamCommand {
    private final Teams teams;

    private AnnotatedTeamCommand(Teams teams) {
        this.teams = teams;
    }

    public static dev.marulabs.commandlib.Command create(Teams teams) {
        return AnnotatedCommands.parser()
                                .registerArgument(Set.class, p -> new ObjectArgument<>(p.name(), teams::byName))
                                .parse(new AnnotatedTeamCommand(teams));
    }

    @Execute
    public void list(CommandContext ctx) {
        ctx.sendMessage("Teams: " + String.join(", ",
                                                teams.byName()
                                                     .keySet()));
    }

    @Subcommand("<team> add")
    @Description("Add a player to the team")
    public void add(CommandContext ctx, Set<String> team, Player player) {
        team.add(player.getName());
        ctx.sendSuccess("Members: " + team);
    }

    @Subcommand("<team> remove")
    @Description("Remove a player from the team")
    public void remove(CommandContext ctx, Set<String> team, Player player) {
        team.remove(player.getName());
        ctx.sendSuccess("Members: " + team);
    }
}
