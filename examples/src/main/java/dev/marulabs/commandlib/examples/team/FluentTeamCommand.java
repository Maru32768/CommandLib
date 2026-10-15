package dev.marulabs.commandlib.examples.team;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.ObjectArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;

import java.util.Set;

/**
 * Use case 3, an argument-first tree: {@code /team}, {@code /team <team> add <player>} and
 * {@code /team <team> remove <player>}. The team is looked up once, by {@link ObjectArgument}, and the children read
 * it through their {@code ArgRef}.
 */
public final class FluentTeamCommand extends Command {
    public FluentTeamCommand(Teams teams) {
        super("team");
        description("Manage team members");

        execute(ctx -> ctx.sendMessage("Teams: " + String.join(", ",
                                                               teams.byName()
                                                                    .keySet())));
        argument(new ObjectArgument<Set<String>>("team", teams::byName)).child(team -> new Command("add") {{
            description("Add a player to the team");
            argument(new PlayerArgument("player")).execute((player, ctx) -> {
                team.get()
                    .add(player.getName());
                ctx.sendSuccess("Members: " + team.get());
            });
        }})
                                                                        .child(team -> new Command("remove") {{
                                                                            description("Remove a player from the team");
                                                                            argument(new PlayerArgument("player")).execute((player,
                                                                                                                            ctx) -> {
                                                                                team.get()
                                                                                    .remove(player.getName());
                                                                                ctx.sendSuccess("Members: "
                                                                                        + team.get());
                                                                            });
                                                                        }});
    }
}
