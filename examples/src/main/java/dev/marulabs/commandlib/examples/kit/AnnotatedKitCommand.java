package dev.marulabs.commandlib.examples.kit;

import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.annotation.Command;
import dev.marulabs.commandlib.annotation.Default;
import dev.marulabs.commandlib.annotation.Description;
import dev.marulabs.commandlib.annotation.Range;
import dev.marulabs.commandlib.annotation.Subcommand;
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
