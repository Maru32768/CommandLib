package dev.marulabs.sampleplugin;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.PlayerArgument;

/**
 * The Java builder style.
 *
 * <pre>
 * /feed [target]
 * </pre>
 */
public final class FeedCommand {
    private FeedCommand() {
    }

    public static Command create() {
        return Command.builder("feed")
                      .description("Fill a player's hunger bar")
                      .addAliases("eat")
                      // An omitted target is the sender, who then has to be a player.
                      .argument(new PlayerArgument("target").optional(Command.player()),
                                branch -> branch.execute((ctx, target) -> {
                                    target.setFoodLevel(20);
                                    ctx.sendSuccess("Fed " + target.getName());
                                }))
                      .build();
    }
}
