package dev.marulabs.commandlib;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BukkitCommandSchedulerTest {
    private final Plugin plugin = mock(Plugin.class);
    private final BukkitScheduler bukkitScheduler = mock(BukkitScheduler.class);
    private final Runnable task = mock(Runnable.class);
    private MockedStatic<Bukkit> bukkit;
    private BukkitCommandScheduler scheduler;

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler)
              .thenReturn(bukkitScheduler);
        scheduler = new BukkitCommandScheduler(plugin);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void sync_task_runs_at_once_on_the_main_thread() {
        bukkit.when(Bukkit::isPrimaryThread)
              .thenReturn(true);

        scheduler.sync(null)
                 .execute(task);

        verify(task).run();
        verifyNoInteractions(bukkitScheduler);
    }

    @Test
    void sync_task_from_another_thread_is_scheduled_on_the_main_thread() {
        bukkit.when(Bukkit::isPrimaryThread)
              .thenReturn(false);

        scheduler.sync(null)
                 .execute(task);

        verifyNoInteractions(task);
        verify(bukkitScheduler).runTask(plugin, task);
    }

    @Test
    void async_work_runs_on_the_async_pool_of_the_plugin() {
        scheduler.async(null)
                 .execute(task);

        verifyNoInteractions(task);
        verify(bukkitScheduler).runTaskAsynchronously(plugin, task);
    }
}
