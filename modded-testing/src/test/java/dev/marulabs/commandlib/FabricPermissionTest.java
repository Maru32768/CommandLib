package dev.marulabs.commandlib;

//? if fabric {
/*import dev.marulabs.commandlib.internal.CurrentServer;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

// CommandTester answers permissions from the FakeSender, so check CommandLib's own Fabric lookup here.
class FabricPermissionTest {
    @Test
    void defaults_decide_without_fabric_permissions_api() {
        CommandLib.register("fabrictest", new Command("open") {{
            permission(DefaultPermission.ALL);
        }}, new Command("closed") {{
            permission(DefaultPermission.NONE);
        }}, new Command("ops"));
        ServerPlayer player = Mockito.mock(ServerPlayer.class);
        Mockito.when(player.hasPermissions(4))
               .thenReturn(true);
        CommandSourceStack source = Mockito.mock(CommandSourceStack.class);

        assertThat(CommandLib.hasPermission(source, player, "fabrictest.open")).isTrue();
        assertThat(CommandLib.hasPermission(source, player, "fabrictest.closed")).isFalse();
        assertThat(CommandLib.hasPermission(source, player, "fabrictest.ops")).isTrue();
        assertThat(CommandLib.hasPermission(source, Mockito.mock(ServerPlayer.class), "fabrictest.ops")).isFalse();
    }

    @Test
    void fabric_permissions_api_decides_with_the_default_as_its_fallback() {
        CommandLib.register("fabricapitest", new Command("open") {{
            permission(DefaultPermission.ALL);
        }});
        ServerPlayer player = Mockito.mock(ServerPlayer.class);
        CommandSourceStack source = Mockito.mock(CommandSourceStack.class);
        FabricLoader loader = Mockito.mock(FabricLoader.class);
        Mockito.when(loader.isModLoaded("fabric-permissions-api-v0"))
               .thenReturn(true);

        try (MockedStatic<FabricLoader> fabricLoader = Mockito.mockStatic(FabricLoader.class);
             MockedStatic<Permissions> permissions = Mockito.mockStatic(Permissions.class)) {
            fabricLoader.when(FabricLoader::getInstance)
                        .thenReturn(loader);
            // Only the right fallback is stubbed to true; any other call answers the mock's false.
            permissions.when(() -> Permissions.check(player, "fabricapitest.open", true))
                       .thenReturn(true);
            permissions.when(() -> Permissions.check(player, "fabricapitest.undeclared", false))
                       .thenReturn(true);

            assertThat(CommandLib.hasPermission(source, player, "fabricapitest.open")).isTrue();
            // An undeclared node falls back to level 4, which the source lacks.
            assertThat(CommandLib.hasPermission(source, player, "fabricapitest.undeclared")).isTrue();
        }
    }

    @Test
    void the_server_is_followed_from_starting_to_stopped_with_the_game_instance_as_fallback() {
        MinecraftServer server = Mockito.mock(MinecraftServer.class);
        MinecraftServer dedicated = Mockito.mock(MinecraftServer.class);
        FabricLoader loader = Mockito.mock(FabricLoader.class);
        CurrentServer.listen();

        ServerLifecycleEvents.SERVER_STARTING.invoker()
                                             .onServerStarting(server);
        assertThat(CurrentServer.get()).isSameAs(server);
        ServerLifecycleEvents.SERVER_STOPPED.invoker()
                                            .onServerStopped(server);

        try (MockedStatic<FabricLoader> fabricLoader = Mockito.mockStatic(FabricLoader.class)) {
            fabricLoader.when(FabricLoader::getInstance)
                        .thenReturn(loader);
            assertThat(CurrentServer.get()).isNull();

            // A copy that missed the event finds a dedicated server as the game instance.
            Mockito.when(loader.getGameInstance())
                   .thenReturn(dedicated);
            assertThat(CurrentServer.get()).isSameAs(dedicated);
        }
    }
}
*///?}
