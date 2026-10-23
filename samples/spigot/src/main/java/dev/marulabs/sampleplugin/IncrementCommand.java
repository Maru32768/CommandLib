package dev.marulabs.sampleplugin;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.StringArgument;

import java.util.HashMap;
import java.util.Map;

/**
 * /increment <target> [count]
 */
public class IncrementCommand extends Command {
    private final Map<String, Integer> values = new HashMap<>();

    public IncrementCommand() {
        super("increment");

        // An omitted count is 1.
        argument(new StringArgument("target", StringArgument.Type.WORD),
                 new IntegerArgument("count").optional(1)).execute((ctx, name, count) -> {
                     int value = values.merge(name, count, Integer::sum);
                     ctx.sendSuccess(name + " = " + value);
                 });
    }
}
