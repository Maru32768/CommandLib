package dev.marulabs.commandlib;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * The scheduler of paper's CommandLib, which lives in the same package.
 */
class BukkitCommandSchedulerTest {
    private final Plugin plugin = mock(Plugin.class);
    private final BukkitScheduler bukkitScheduler = mock(BukkitScheduler.class);
    private final Runnable task = mock(Runnable.class);
    private final List<String> lookups = new ArrayList<>();
    private MockedStatic<Bukkit> bukkit;
    private BukkitCommandScheduler scheduler;

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler)
              .thenReturn(bukkitScheduler);
        scheduler = new BukkitCommandScheduler(() -> {
            lookups.add("plugin");
            return plugin;
        });
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
    void the_plugin_is_looked_up_when_a_task_is_scheduled() {
        // A bootstrapper registers commands before the plugin exists.
        assertThat(lookups).isEmpty();

        scheduler.async(null)
                 .execute(task);

        assertThat(lookups).containsExactly("plugin");
        verify(bukkitScheduler).runTaskAsynchronously(plugin, task);
    }
}
