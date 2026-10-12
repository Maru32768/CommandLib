package dev.marulabs.samplemod;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.IntegerArgument;

/**
 * /math add <left> <right>
 * /math multiply <left> <right>
 */
public class MathCommand extends Command {
    public MathCommand() {
        super("math");

        addChildren(operation("add", (left, right) -> left + right),
                    operation("multiply", (left, right) -> left * right));
    }

    private Command operation(String name, IntOperation operation) {
        return new Command(name) {{
            argument(new IntegerArgument("left"), new IntegerArgument("right")).execute((left, right, ctx) -> {
                ctx.sendSuccess(operation.apply(left, right));
            });
        }};
    }

    private interface IntOperation {
        int apply(int left, int right);
    }
}
