package dev.marulabs.commandlib;

import net.minecraft.server.MinecraftServer;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs async commands on the server: sync tasks at once on the server thread and through the server of the command's
 * source from another thread, which queues them for the next tick, and async work on a pool of daemon threads that this
 * copy of CommandLib owns. The pool grows with the work, since the work, such as file or database access,
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
        MinecraftServer server = ((CommandContext) ctx).getSender()
                                                       .getServer();
        return task -> runOnServerThread(server, task);
    }

    /**
     * Runs {@code task} at once on the server thread and queues it from another thread. {@link MinecraftServer#execute}
     * alone also queues a task while the server runs one, such as the packet of a command a player typed.
     */
    static void runOnServerThread(MinecraftServer server, Runnable task) {
        if (server.isSameThread()) {
            task.run();
        } else {
            server.execute(task);
        }
    }

    @Override
    public Executor async(CommonCommandContext<?, ?> ctx) {
        return async;
    }
}
