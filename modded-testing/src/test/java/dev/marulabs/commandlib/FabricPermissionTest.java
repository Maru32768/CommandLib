package dev.marulabs.commandlib;

//? if fabric {
/*import dev.marulabs.commandlib.internal.CurrentServer;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.transformer.EnvironmentStrippingData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

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

            // The server stays in a static field that the other test classes in this JVM read.
            try {
                ServerLifecycleEvents.SERVER_STARTING.invoker()
                                                     .onServerStarting(server);
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

    // The test classpath has the client classes, so CurrentServer and the CommandLib classes it reaches are loaded
    // again without what a dedicated server lacks: Minecraft's client packages, which the server jar does not have,
    // and the classes that Fabric Loader refuses to load there. A client class that their verification, static
    // initializers, get() or lifecycle listeners need then fails here as it does on a server.
    @Test
    void current_server_loads_no_client_class_on_a_dedicated_server() throws Exception {
        MinecraftServer dedicated = Mockito.mock(MinecraftServer.class);
        FabricLoader loader = Mockito.mock(FabricLoader.class);
        Mockito.when(loader.getGameInstance())
               .thenReturn(dedicated);
        ClassLoader withoutClient = new ClassLoader(FabricPermissionTest.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("net.minecraft.client.") || name.startsWith("net.minecraft.realms.")
                    || name.startsWith("com.mojang.blaze3d.") || name.startsWith("com.mojang.realmsclient.")) {
                    throw new ClassNotFoundException(name);
                }
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded != null) {
                        return loaded;
                    }
                    byte[] bytes;
                    try (InputStream in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                        if (in == null) {
                            throw new ClassNotFoundException(name);
                        }
                        bytes = in.readAllBytes();
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }
                    EnvironmentStrippingData stripping =
                        new EnvironmentStrippingData(Opcodes.ASM9, EnvType.SERVER.name());
                    new ClassReader(bytes).accept(stripping, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
                    if (stripping.stripEntireClass()) {
                        throw new ClassNotFoundException(name);
                    }
                    if (!name.startsWith("dev.marulabs.commandlib.")) {
                        return super.loadClass(name, resolve);
                    }
                    return defineClass(name, bytes, 0, bytes.length);
                }
            }
        };

        try (MockedStatic<FabricLoader> fabricLoader = Mockito.mockStatic(FabricLoader.class)) {
            fabricLoader.when(FabricLoader::getInstance)
                        .thenReturn(loader);
            Class<?> copy = Class.forName(CurrentServer.class.getName(), true, withoutClient);
            assertThat(copy.getClassLoader()).isSameAs(withoutClient);
            Method get = copy.getMethod("get");
            assertThat(get.invoke(null)).isSameAs(dedicated);
            try {
                ServerLifecycleEvents.SERVER_STARTING.invoker()
                                                     .onServerStarting(dedicated);
                assertThat(get.invoke(null)).isSameAs(dedicated);
            } finally {
                ServerLifecycleEvents.SERVER_STOPPED.invoker()
                                                    .onServerStopped(dedicated);
            }
        }
    }
}
*///?}
