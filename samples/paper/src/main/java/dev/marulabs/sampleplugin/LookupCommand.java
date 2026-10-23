package dev.marulabs.sampleplugin;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.StringArgument;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * /lookup <name>: looks the name up off the server thread and logs how long it took.
 */
public class LookupCommand extends Command {
    public LookupCommand(Logger logger) {
        super("lookup");

        // A middleware wraps the execution. The stage proceed() returns completes when the async work is done.
        addMiddleware((ctx, next) -> {
            long start = System.nanoTime();
            return next.proceed()
                       .whenComplete((ignored, e) -> logger.info("/lookup took "
                               + (System.nanoTime() - start) / 1_000_000 + " ms"));
        });

        // supplyAsync runs the work off the server thread and completes back on it, so thenAccept may send messages.
        argument(new StringArgument("name", StringArgument.Type.WORD)).executeAsync((ctx, name) -> {
            return ctx.supplyAsync(() -> slowLookup(name))
                      .thenAccept(result -> ctx.sendSuccess(name + " -> " + result));
        });
    }

    private static String slowLookup(String name) {
        try {
            // Stands in for a database or web request.
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
        }
        return name.toUpperCase(Locale.ROOT);
    }
}
