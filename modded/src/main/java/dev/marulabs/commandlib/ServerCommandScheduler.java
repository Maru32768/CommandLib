package dev.marulabs.commandlib;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs async commands on the server: sync tasks through the server of the command's source, which runs a task at once
 * on the server thread and queues it for the next tick from another thread, and async work on a pool of daemon threads
 * that this copy of CommandLib owns. The pool grows with the work, since the work, such as file or database access,
 * usually blocks.
 */
final class ServerCommandScheduler implements CommandScheduler {
    static final ServerCommandScheduler INSTANCE = new ServerCommandScheduler();
    private static final AtomicInteger THREAD_COUNT = new AtomicInteger();
    private final ExecutorService async = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "CommandLib Async #" + THREAD_COUNT.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });

    private ServerCommandScheduler() {
    }

    @Override
    public Executor sync(CommonCommandContext<?, ?> ctx) {
        return ((CommandContext) ctx).getSender()
                                     .getServer();
    }

    @Override
    public Executor async(CommonCommandContext<?, ?> ctx) {
        return async;
    }
}
