package dev.marulabs.commandlib.examples.kit;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;
import dev.marulabs.commandlib.argument.StringArgument;
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
                                                 branch -> branch.execute((name, count, ctx) -> {
                                                     ctx.sendSuccess("Gave " + count + " x " + name);
                                                 })))
                      .addChild(Command.builder("send")
                                       .description("Send the starter kit to players")
                                       .argument(new PlayerArgument("players").variadic(),
                                                 branch -> branch.execute((players, ctx) -> {
                                                     ctx.sendSuccess("Sent the starter kit to " + players.stream()
                                                                                                         .map(Player::getName)
                                                                                                         .collect(Collectors.joining(", ")));
                                                 })))
                      .build();
    }
}
