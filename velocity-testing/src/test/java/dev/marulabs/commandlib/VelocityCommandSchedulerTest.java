package dev.marulabs.commandlib;

import com.velocitypowered.api.proxy.ProxyServer;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/**
 * The scheduler of velocity's CommandLib, which lives in the same package.
 */
class VelocityCommandSchedulerTest {
    private final Object plugin = new Object();
    private final ProxyServer proxy = mock(ProxyServer.class, RETURNS_DEEP_STUBS);
    private final Runnable task = mock(Runnable.class);
    private final VelocityCommandScheduler scheduler = new VelocityCommandScheduler(plugin, proxy);

    @Test
    void sync_task_runs_at_once_since_the_proxy_has_no_main_thread() {
        scheduler.sync(null)
                 .execute(task);

        verify(task).run();
    }

    @Test
    void async_work_goes_to_the_proxy_scheduler_of_the_plugin() {
        scheduler.async(null)
                 .execute(task);

        verifyNoInteractions(task);
        verify(proxy.getScheduler()
                    .buildTask(plugin, task)).schedule();
    }
}
