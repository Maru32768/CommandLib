package dev.marulabs.samplemod;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.StringArgument;

/**
 * /topic <name>
 */
public class SuggestCommand extends Command {
    public SuggestCommand() {
        super("topic");

        argument(new StringArgument("name", StringArgument.Type.WORD).addSuggestionProvider(sb -> {
            sb.suggest("spawn", "Main spawn area");
            sb.suggest("arena", "PvP arena");
            sb.suggest("shop", "Server shop");
        })).execute((ctx, name) -> {
            ctx.sendSuccess("Selected topic: " + name);
        });
    }
}
