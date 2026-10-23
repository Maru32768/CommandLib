package dev.marulabs.commandlib.examples.kit;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;
import dev.marulabs.commandlib.argument.StringArgument;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

/**
 * Use case 2, optional and variadic arguments: {@code /kit give <name> [count=1]} and {@code /kit send <players...>}.
 */
public final class FluentKitCommand extends Command {
    public FluentKitCommand() {
        super("kit");
        description("Hand out kits");

        addChildren(new Command("give") {{
            description("Give yourself a kit");
            argument(new StringArgument("name"),
                     new IntegerArgument("count", 1, 64).optional(1)).execute((ctx, name, count) -> {
                         ctx.sendSuccess("Gave " + count + " x " + name);
                     });
        }}, new Command("send") {{
            description("Send the starter kit to players");
            argument(new PlayerArgument("players").variadic()).execute((ctx, players) -> {
                ctx.sendSuccess("Sent the starter kit to " + players.stream()
                                                                    .map(Player::getName)
                                                                    .collect(Collectors.joining(", ")));
            });
        }});
    }
}
