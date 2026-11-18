package dev.marulabs.cadence.examples.kit;

import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.annotation.Command;
import dev.marulabs.cadence.annotation.Default;
import dev.marulabs.cadence.annotation.Description;
import dev.marulabs.cadence.annotation.Range;
import dev.marulabs.cadence.annotation.Subcommand;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link FluentKitCommand} written with annotations.
 */
@Command("kit")
@Description("Hand out kits")
public final class AnnotatedKitCommand {
    @Subcommand("give")
    @Description("Give yourself a kit")
    public void give(CommandContext ctx, String name, @Range(min = 1, max = 64) @Default("1") int count) {
        ctx.sendSuccess("Gave " + count + " x " + name);
    }

    @Subcommand("send")
    @Description("Send the starter kit to players")
    public void send(CommandContext ctx, List<Player> players) {
        ctx.sendSuccess("Sent the starter kit to " + players.stream()
                                                            .map(Player::getName)
                                                            .collect(Collectors.joining(", ")));
    }
}
