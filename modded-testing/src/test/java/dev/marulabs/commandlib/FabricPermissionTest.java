package dev.marulabs.commandlib;

//? if fabric {
/*import dev.marulabs.commandlib.internal.CurrentServer;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

// CommandTester answers permissions from the FakeSender, so check CommandLib's own Fabric lookup here.
class FabricPermissionTest {
    FabricPermissionTest() {
        CommandTester.MinecraftBootstrap.ensureBootstrapped();
    }

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
    void the_server_is_followed_from_starting_to_stopped() {
        MinecraftServer server = Mockito.mock(MinecraftServer.class);
        FabricLoader loader = Mockito.mock(FabricLoader.class);
        // On a client the server thread can run SERVER_STARTING before Minecraft holds the integrated server.
        Mockito.when(loader.getGameInstance())
               .thenReturn(Mockito.mock(Minecraft.class));

        try (MockedStatic<FabricLoader> fabricLoader = Mockito.mockStatic(FabricLoader.class)) {
            fabricLoader.when(FabricLoader::getInstance)
                        .thenReturn(loader);
            assertThat(CurrentServer.get()).isNull();

            ServerLifecycleEvents.SERVER_STARTING.invoker()
                                                 .onServerStarting(server);
            // The server stays in a static field that the other test classes in this JVM read.
            try {
                assertThat(CurrentServer.get()).isSameAs(server);
            } finally {
                ServerLifecycleEvents.SERVER_STOPPED.invoker()
                                                    .onServerStopped(server);
            }
            assertThat(CurrentServer.get()).isNull();
        }
    }

    @Test
    void a_copy_that_missed_the_events_finds_a_dedicated_server_as_the_game_instance() {
        MinecraftServer dedicated = Mockito.mock(MinecraftServer.class);
        FabricLoader loader = Mockito.mock(FabricLoader.class);
        Mockito.when(loader.getGameInstance())
               .thenReturn(dedicated);

        try (MockedStatic<FabricLoader> fabricLoader = Mockito.mockStatic(FabricLoader.class)) {
            fabricLoader.when(FabricLoader::getInstance)
                        .thenReturn(loader);
            assertThat(CurrentServer.get()).isSameAs(dedicated);
        }
    }

    @Test
    void a_copy_that_missed_the_events_finds_the_integrated_server_of_a_client() {
        IntegratedServer integrated = Mockito.mock(IntegratedServer.class);
        Minecraft client = Mockito.mock(Minecraft.class);
        Mockito.when(client.getSingleplayerServer())
               .thenReturn(integrated);
        FabricLoader loader = Mockito.mock(FabricLoader.class);
        Mockito.when(loader.getGameInstance())
               .thenReturn(client);

        try (MockedStatic<FabricLoader> fabricLoader = Mockito.mockStatic(FabricLoader.class)) {
            fabricLoader.when(FabricLoader::getInstance)
                        .thenReturn(loader);
            assertThat(CurrentServer.get()).isSameAs(integrated);
        }
    }

    // A dedicated server has no client classes, and the verifier loads the classes a method's types need. The test
    // classpath has them, so only the class files can show which class needs them.
    @Test
    void current_server_refers_to_client_classes_only_from_its_nested_class() throws IOException {
        boolean server = classFile("CurrentServer").contains("net/minecraft/client/");
        boolean client = classFile("CurrentServer$Client").contains("net/minecraft/client/");
        assertThat(server).as("CurrentServer refers to a client class")
                          .isFalse();
        assertThat(client).as("CurrentServer$Client refers to a client class")
                          .isTrue();
    }

    private static String classFile(String name) throws IOException {
        try (InputStream in = CurrentServer.class.getResourceAsStream(name + ".class")) {
            return new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }
}
*///?}
