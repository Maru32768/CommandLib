package dev.marulabs.commandlib;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Runs async commands with the Bukkit scheduler of the plugin that registered them: sync tasks on the main thread, at
 * once when already there, and async work on the scheduler's async pool. The scheduler refuses tasks of a disabled
 * plugin, so the follow-up of a command still running when its plugin is disabled is dropped. Folia has no such
 * scheduler.
 */
final class BukkitCommandScheduler implements CommandScheduler {
    private final Executor sync;
    private final Executor async;

    /**
     * @param plugin the plugin, looked up when a task is scheduled, since a bootstrapper registers commands before the
     *               plugin exists
     */
    BukkitCommandScheduler(Supplier<Plugin> plugin) {
        this.sync = task -> {
            if (Bukkit.isPrimaryThread()) {
                task.run();
            } else {
                Bukkit.getScheduler()
                      .runTask(Objects.requireNonNull(plugin.get()), task);
            }
        };
        this.async = task -> Bukkit.getScheduler()
                                   .runTaskAsynchronously(Objects.requireNonNull(plugin.get()), task);
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
