package dev.marulabs.samplemod;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.StringArgument;

import java.util.HashMap;
import java.util.Map;

/**
 * /increment <target>
 * /increment <target> <count>
 */
public class IncrementCommand extends Command {
    private final Map<String, Integer> values = new HashMap<>();

    public IncrementCommand() {
        super("increment");

        StringArgument target = new StringArgument("target", StringArgument.Type.WORD);
        argument(target).execute((name, ctx) -> increment(name, 1, ctx));

        argument(target, new IntegerArgument("count")).execute((name, count, ctx) -> increment(name, count, ctx));
    }

    private void increment(String name, int count, CommandContext ctx) {
        int value = values.getOrDefault(name, 0) + count;
        values.put(name, value);
        ctx.sendSuccess(name + " = " + value);
    }
}
