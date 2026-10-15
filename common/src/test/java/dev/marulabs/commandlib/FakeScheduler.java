package dev.marulabs.commandlib;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;

/**
 * A scheduler the test drives by hand. The thread that creates it is the sync thread: the sync executor runs a task at
 * once there, as the platforms' executors do, and queues it when called from an async task or another thread.
 */
final class FakeScheduler implements CommandScheduler {
    private final Thread syncThread = Thread.currentThread();
    private final Deque<Runnable> syncTasks = new ArrayDeque<>();
    private final Deque<Runnable> asyncTasks = new ArrayDeque<>();
    private boolean inAsyncTask = false;

    @Override
    public Executor sync(CommonCommandContext<?, ?> ctx) {
        return task -> {
            if (isSyncThread()) {
                task.run();
            } else {
                synchronized (syncTasks) {
                    syncTasks.add(task);
                }
            }
        };
    }

    @Override
    public Executor async(CommonCommandContext<?, ?> ctx) {
        return asyncTasks::add;
    }

    boolean isSyncThread() {
        return Thread.currentThread() == syncThread && !inAsyncTask;
    }

    void runAsyncTasks() {
        inAsyncTask = true;
        try {
            Runnable task;
            while ((task = asyncTasks.poll()) != null) {
                task.run();
            }
        } finally {
            inAsyncTask = false;
        }
    }

    void runSyncTasks() {
        while (true) {
            Runnable task;
            synchronized (syncTasks) {
                task = syncTasks.poll();
            }
            if (task == null) {
                return;
            }
            task.run();
        }
    }

    /**
     * Runs the queued work as the server would: the async tasks, then what they handed back to the sync thread.
     */
    void runAll() {
        runAsyncTasks();
        runSyncTasks();
    }
}
