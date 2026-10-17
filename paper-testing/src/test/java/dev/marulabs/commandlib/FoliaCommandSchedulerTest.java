package dev.marulabs.commandlib;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * The scheduler of paper's CommandLib on Folia, with Folia's schedulers mocked.
 */
@SuppressWarnings({"UnstableApiUsage", "unchecked"})
class FoliaCommandSchedulerTest {
    private final Plugin plugin = mock(Plugin.class);
    private final Runnable task = mock(Runnable.class);
    private final RegionScheduler regionScheduler = mock(RegionScheduler.class);
    private final GlobalRegionScheduler globalScheduler = mock(GlobalRegionScheduler.class);
    private final AsyncScheduler asyncScheduler = mock(AsyncScheduler.class);
    private boolean globalTickThread;
    private final FoliaCommandScheduler scheduler = new FoliaCommandScheduler(() -> plugin, () -> globalTickThread);
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getRegionScheduler)
              .thenReturn(regionScheduler);
        bukkit.when(Bukkit::getGlobalRegionScheduler)
              .thenReturn(globalScheduler);
        bukkit.when(Bukkit::getAsyncScheduler)
              .thenReturn(asyncScheduler);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void the_test_server_is_not_folia() {
        assertThat(FoliaCommandScheduler.ON_FOLIA).isFalse();
        assertThat(FoliaCommandScheduler.forServer(() -> plugin)).isInstanceOf(BukkitCommandScheduler.class);
    }

    @Test
    void sync_task_runs_at_once_on_the_thread_of_the_entity() {
        Player player = mock(Player.class);
        bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(player))
              .thenReturn(true);

        scheduler.sync(context(player, player))
                 .execute(task);

        verify(task).run();
        verify(player, never()).getScheduler();
    }

    @Test
    void sync_task_from_another_thread_goes_to_the_entity_the_command_runs_as() {
        // /execute as <entity> run ..., sent by the console.
        Entity entity = mock(Entity.class);
        EntityScheduler entityScheduler = mock(EntityScheduler.class);
        when(entity.getScheduler()).thenReturn(entityScheduler);
        when(entityScheduler.execute(eq(plugin), eq(task), any(), eq(1L))).thenReturn(true);

        scheduler.sync(context(mock(ConsoleCommandSender.class), entity))
                 .execute(task);

        verifyNoInteractions(task, globalScheduler);
        ArgumentCaptor<Runnable> retired = ArgumentCaptor.forClass(Runnable.class);
        verify(entityScheduler).execute(eq(plugin), eq(task), retired.capture(), eq(1L));

        // The entity is removed before the task runs.
        retired.getValue()
               .run();
        verify(globalScheduler).execute(plugin, task);
    }

    @Test
    void sync_task_of_an_entity_already_removed_goes_to_the_global_region() {
        Entity entity = mock(Entity.class);
        EntityScheduler entityScheduler = mock(EntityScheduler.class);
        when(entity.getScheduler()).thenReturn(entityScheduler);

        scheduler.sync(context(mock(ConsoleCommandSender.class), entity))
                 .execute(task);

        verify(globalScheduler).execute(plugin, task);
        verifyNoInteractions(task);
    }

    @Test
    void sync_task_of_a_command_block_goes_to_its_region() {
        Location location = mock(Location.class);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        BlockCommandSender sender = mock(BlockCommandSender.class);
        when(sender.getBlock()).thenReturn(block);

        scheduler.sync(context(sender, null))
                 .execute(task);
        verify(regionScheduler).execute(plugin, location, task);

        bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(location))
              .thenReturn(true);
        scheduler.sync(context(sender, null))
                 .execute(task);
        verify(task).run();
    }

    @Test
    void sync_task_of_the_console_goes_to_the_global_region() {
        CommonCommandContext<?, ?> console = context(mock(ConsoleCommandSender.class), null);

        scheduler.sync(console)
                 .execute(task);
        verify(globalScheduler).execute(plugin, task);
        verifyNoInteractions(task);

        globalTickThread = true;
        scheduler.sync(console)
                 .execute(task);
        verify(task).run();
    }

    @Test
    void async_work_runs_on_the_async_scheduler() {
        scheduler.async(context(mock(ConsoleCommandSender.class), null))
                 .execute(task);

        ArgumentCaptor<Consumer<ScheduledTask>> work = ArgumentCaptor.forClass(Consumer.class);
        verify(asyncScheduler).runNow(eq(plugin), work.capture());
        verifyNoInteractions(task);
        work.getValue()
            .accept(mock(ScheduledTask.class));
        verify(task).run();
    }

    private static CommandContext context(CommandSender sender, Entity executor) {
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.getSender()).thenReturn(sender);
        when(source.getExecutor()).thenReturn(executor);
        com.mojang.brigadier.context.CommandContext<CommandSourceStack> handle = mock(com.mojang.brigadier.context.CommandContext.class);
        when(handle.getSource()).thenReturn(source);
        return new CommandContext(handle);
    }
}
