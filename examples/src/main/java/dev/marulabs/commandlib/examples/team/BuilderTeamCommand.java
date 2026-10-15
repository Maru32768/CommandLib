package dev.marulabs.commandlib.examples.team;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.ObjectArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;

import java.util.Set;

/**
 * {@link FluentTeamCommand} written with the Java builder.
 */
public final class BuilderTeamCommand {
    private BuilderTeamCommand() {
    }

    public static Command create(Teams teams) {
        return Command.builder("team")
                      .description("Manage team members")
                      .execute(ctx -> ctx.sendMessage("Teams: " + String.join(", ",
                                                                              teams.byName()
                                                                                   .keySet())))
                      .argument(new ObjectArgument<Set<String>>("team", teams::byName),
                                branch -> branch.child(team -> Command.builder("add")
                                                                      .description("Add a player to the team")
                                                                      .argument(new PlayerArgument("player"),
                                                                                b -> b.execute((player, ctx) -> {
                                                                                    team.get()
                                                                                        .add(player.getName());
                                                                                    ctx.sendSuccess("Members: "
                                                                                            + team.get());
                                                                                }))
                                                                      .build())
                                                .child(team -> Command.builder("remove")
                                                                      .description("Remove a player from the team")
                                                                      .argument(new PlayerArgument("player"),
                                                                                b -> b.execute((player, ctx) -> {
                                                                                    team.get()
                                                                                        .remove(player.getName());
                                                                                    ctx.sendSuccess("Members: "
                                                                                            + team.get());
                                                                                }))
                                                                      .build()))
                      .build();
    }
}
