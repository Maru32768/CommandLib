package dev.marulabs.cadence.examples.kit;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.argument.IntegerArgument;
import dev.marulabs.cadence.argument.PlayerArgument;
import dev.marulabs.cadence.argument.StringArgument;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

/**
 * {@link FluentKitCommand} written with the Java builder.
 */
public final class BuilderKitCommand {
    private BuilderKitCommand() {
    }

    public static Command create() {
        return Command.builder("kit")
                      .description("Hand out kits")
                      .addChild(Command.builder("give")
                                       .description("Give yourself a kit")
                                       .argument(new StringArgument("name"),
                                                 new IntegerArgument("count", 1, 64).optional(1),
                                                 branch -> branch.execute((ctx, name, count) -> {
                                                     ctx.sendSuccess("Gave " + count + " x " + name);
                                                 })))
                      .addChild(Command.builder("send")
                                       .description("Send the starter kit to players")
                                       .argument(new PlayerArgument("players").variadic(),
                                                 branch -> branch.execute((ctx, players) -> {
                                                     ctx.sendSuccess("Sent the starter kit to " + players.stream()
                                                                                                         .map(Player::getName)
                                                                                                         .collect(Collectors.joining(", ")));
                                                 })))
                      .build();
    }
}
