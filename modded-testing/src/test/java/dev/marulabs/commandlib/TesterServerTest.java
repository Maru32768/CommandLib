package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.PlayerArgument;
import dev.marulabs.commandlib.internal.CurrentServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
//? if neoforge {
/*import net.neoforged.neoforge.server.ServerLifecycleHooks;
*///?} elif forge && >=1.17 {
import net.minecraftforge.server.ServerLifecycleHooks;
//?} elif forge
/*import net.minecraftforge.fml.server.ServerLifecycleHooks;*/

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

// What a command sees of the tester's server.
class TesterServerTest {
    @Test
    void the_source_is_in_the_overworld() {
        List<String> levels = new ArrayList<>();
        try (CommandTester tester = new CommandTester(new Command("where") {{
            execute(ctx -> {
                levels.add(ctx.getWorld()
                              .dimension()
                              .location()
                              .toString());
                levels.add(ctx.getLocation()
                              .getWorld()
                              .dimension()
                              .location()
                              .toString());
            });
        }}, "test")) {
            tester.execute("where", FakeSender.player("Steve"));
            tester.execute("where", FakeSender.console());
        }
        assertThat(levels).containsOnly(Level.OVERWORLD.location()
                                                       .toString())
                          .hasSize(4);
    }

    @Test
    void the_nearest_player_resolves_among_several() {
        List<String> nearest = new ArrayList<>();
        try (CommandTester tester = new CommandTester(new Command("nearest") {{
            argument(new PlayerArgument("p")).execute((ctx, p) -> nearest.add(p.getScoreboardName()));
        }}, "test").withPlayer(at("Alex", 25))
                   .withPlayer(at("Bob", 1))
                   .withPlayer(at("Carl", 9))) {
            tester.execute("nearest @p", FakeSender.console());
            // A player is nearest to itself.
            tester.execute("nearest @p",
                           FakeSender.player("Steve")
                                     .op(true));
        }
        assertThat(nearest).containsExactly("Bob", "Steve");
    }

    // A player at the given squared distance from the console's source, which stands at the origin.
    private static FakeSender at(String name, double distanceSqr) {
        FakeSender player = FakeSender.player(name);
        Mockito.when(player.asPlayer()
                           .orElseThrow()
                           .distanceToSqr(Mockito.any(Vec3.class)))
               .thenReturn(distanceSqr);
        return player;
    }

    @Test
    void the_running_server_is_the_tester_server() {
        List<MinecraftServer> servers = new ArrayList<>();
        try (CommandTester tester = new CommandTester(new Command("server") {{
            execute(ctx -> {
                servers.add(ctx.getSender()
                               .getServer());
                servers.add(CurrentServer.get());
                //? if forge || neoforge
                servers.add(ServerLifecycleHooks.getCurrentServer());
            });
        }}, "test")) {
            tester.execute("server", FakeSender.player("Steve"));
        }
        assertThat(servers).doesNotContainNull()
                           .containsOnly(servers.get(0));
    }

    @Test
    void refresh_sends_the_command_tree() {
        List<MinecraftServer> servers = new ArrayList<>();
        FakeSender steve = FakeSender.player("Steve");
        try (CommandTester tester = new CommandTester(new Command("refresh") {{
            execute(ctx -> {
                CommandLib.refresh(steve.asPlayer()
                                        .orElseThrow());
                CommandLib.refreshAll();
            });
        }}, "test").withServer(servers::add)) {
            tester.execute("refresh", steve);
        }
        Mockito.verify(servers.get(0)
                              .getCommands(),
                       Mockito.times(2))
               .sendCommands(steve.asPlayer()
                                  .orElseThrow());
    }

    @Test
    void a_task_queued_by_the_command_still_sees_the_server_after_execute_returns() {
        Queue<Runnable> asyncTasks = new ArrayDeque<>();
        AtomicReference<MinecraftServer> server = new AtomicReference<>();
        AtomicReference<Boolean> permitted = new AtomicReference<>();
        try (CommandTester tester = CommandTester.builder()
                                                 .command(new Command("later") {{
                                                     executeAsync(ctx -> ctx.runAsync(() -> {
                                                         server.set(CurrentServer.get());
                                                         permitted.set(ctx.getActor()
                                                                          .hasPermission("test.later.extra"));
                                                     }));
                                                 }})
                                                 .permissionPrefix("test")
                                                 .scheduler(CommandScheduler.of(Runnable::run, asyncTasks::add))
                                                 .build()) {
            tester.execute("later", FakeSender.player("Steve"));
            assertThat(server.get()).isNull();

            asyncTasks.forEach(Runnable::run);
        }
        assertThat(server.get()).isNotNull();
        // Answered by the FakeSender, which has every node, not by the loader's lookup for a non-operator.
        assertThat(permitted.get()).isTrue();
    }
}
