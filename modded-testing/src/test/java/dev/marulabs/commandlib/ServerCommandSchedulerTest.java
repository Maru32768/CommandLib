package dev.marulabs.commandlib;

import com.mojang.brigadier.context.StringRange;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServerCommandSchedulerTest {
    @Test
    void sync_tasks_go_to_the_server_of_the_source() {
        MinecraftServer server = mock(MinecraftServer.class);
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.getServer()).thenReturn(server);
        CommandContext ctx = new CommandContext(new com.mojang.brigadier.context.CommandContext<>(source,
                                                                                                  "",
                                                                                                  Map.of(),
                                                                                                  null,
                                                                                                  null,
                                                                                                  List.of(),
                                                                                                  StringRange.at(0),
                                                                                                  null,
                                                                                                  null,
                                                                                                  false));

        // MinecraftServer runs a task at once on the server thread and queues it from another thread.
        assertThat(ServerCommandScheduler.INSTANCE.sync(ctx)).isSameAs(server);
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
