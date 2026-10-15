package dev.marulabs.commandlib;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.Executor;

/**
 * Runs async commands with the Bukkit scheduler of the plugin that registered them: sync tasks on the main thread, at
 * once when already there, and async work on the scheduler's async pool. The scheduler refuses tasks of a disabled
 * plugin, so the follow-up of a command still running when its plugin is disabled is dropped.
 */
final class BukkitCommandScheduler implements CommandScheduler {
    private final Executor sync;
    private final Executor async;

    BukkitCommandScheduler(Plugin plugin) {
        this.sync = task -> {
            if (Bukkit.isPrimaryThread()) {
                task.run();
            } else {
                Bukkit.getScheduler()
                      .runTask(plugin, task);
            }
        };
        this.async = task -> Bukkit.getScheduler()
                                   .runTaskAsynchronously(plugin, task);
    }

    @Override
    public Executor sync(CommonCommandContext<?, ?> ctx) {
        return sync;
    }

    @Override
    public Executor async(CommonCommandContext<?, ?> ctx) {
        return async;
    }
}
