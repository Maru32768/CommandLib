package dev.marulabs.commandlib.examples.team;

import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.annotation.AnnotatedCommands;
import dev.marulabs.commandlib.annotation.Command;
import dev.marulabs.commandlib.annotation.Description;
import dev.marulabs.commandlib.annotation.Execute;
import dev.marulabs.commandlib.argument.ObjectArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;

import java.util.Set;

/**
 * {@link FluentTeamCommand} with annotations. {@code @Subcommand} paths are literals, so annotations cannot put a
 * child after an argument; {@link #create} parses the class and adds the argument-first part with the fluent API.
 */
@Command("team")
@Description("Manage team members")
public final class AnnotatedTeamCommand {
    private final Teams teams;

    private AnnotatedTeamCommand(Teams teams) {
        this.teams = teams;
    }

    public static dev.marulabs.commandlib.Command create(Teams teams) {
        dev.marulabs.commandlib.Command command = AnnotatedCommands.parse(new AnnotatedTeamCommand(teams));
        command.argument(new ObjectArgument<Set<String>>("team", teams::byName))
               .child(team -> new dev.marulabs.commandlib.Command("add") {{
                   description("Add a player to the team");
                   argument(new PlayerArgument("player")).execute((player, ctx) -> {
                       team.get()
                           .add(player.getName());
                       ctx.sendSuccess("Members: " + team.get());
                   });
               }})
               .child(team -> new dev.marulabs.commandlib.Command("remove") {{
                   description("Remove a player from the team");
                   argument(new PlayerArgument("player")).execute((player, ctx) -> {
                       team.get()
                           .remove(player.getName());
                       ctx.sendSuccess("Members: " + team.get());
                   });
               }});
        return command;
    }

    @Execute
    public void list(CommandContext ctx) {
        ctx.sendMessage("Teams: " + String.join(", ",
                                                teams.byName()
                                                     .keySet()));
    }
}
