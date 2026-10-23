package dev.marulabs.commandlib;

import dev.marulabs.commandlib.internal.ContextState;
import com.mojang.brigadier.context.StringRange;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServerCommandSchedulerTest {
    @Test
    void sync_tasks_run_at_once_on_the_server_thread() {
        MinecraftServer server = mock(MinecraftServer.class);
        when(server.isSameThread()).thenReturn(true);
        Runnable task = mock(Runnable.class);

        // MinecraftServer#execute would queue the task while the server runs another, such as a command packet.
        ServerCommandScheduler.INSTANCE.sync(context(server))
                                       .execute(task);

        verify(task).run();
        verify(server, never()).execute(any());
    }

    @Test
    void sync_tasks_from_another_thread_go_to_the_server_of_the_source() {
        MinecraftServer server = mock(MinecraftServer.class);
        Runnable task = mock(Runnable.class);

        ServerCommandScheduler.INSTANCE.sync(context(server))
                                       .execute(task);

        verify(server).execute(task);
        verify(task, never()).run();
    }

    private static CommandContext context(MinecraftServer server) {
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.getServer()).thenReturn(server);
        return new CommandContext(new com.mojang.brigadier.context.CommandContext<>(source,
                                                                                    "",
                                                                                    Map.of(),
                                                                                    null,
                                                                                    null,
                                                                                    List.of(),
                                                                                    StringRange.at(0),
                                                                                    null,
                                                                                    null,
                                                                                    false),
                                  ContextState.EMPTY);
    }

    @Test
    void async_work_runs_on_a_daemon_thread() {
        CompletableFuture<Thread> thread = CompletableFuture.supplyAsync(Thread::currentThread,
                                                                         ServerCommandScheduler.INSTANCE.async(null));

        assertThat(thread.join()
                         .isDaemon()).isTrue();
        assertThat(thread.join()
                         .getName()).startsWith("CommandLib Async #");
    }
}
