package dev.marulabs.sampleplugin;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.argument.StringArgument;

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
