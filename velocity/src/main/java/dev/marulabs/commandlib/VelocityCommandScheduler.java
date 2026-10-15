package dev.marulabs.commandlib;

import com.velocitypowered.api.proxy.ProxyServer;

import java.util.concurrent.Executor;

/**
 * Runs async commands on Velocity. The proxy has no main thread and its API may be used from any thread, so sync tasks
 * run at once on the calling thread. Async work goes to the proxy's scheduler under the plugin, which cancels the
 * plugin's tasks when the plugin is unloaded.
 */
final class VelocityCommandScheduler implements CommandScheduler {
    private final Executor async;

    VelocityCommandScheduler(Object plugin, ProxyServer proxy) {
        this.async = task -> proxy.getScheduler()
                                  .buildTask(plugin, task)
                                  .schedule();
    }

    @Override
    public Executor sync(CommonCommandContext<?, ?> ctx) {
        return Runnable::run;
    }

    @Override
    public Executor async(CommonCommandContext<?, ?> ctx) {
        return async;
    }
}
