package dev.marulabs.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import dev.marulabs.commandlib.MinecraftNamespacedCommandRemovalTest.FakeVanillaCommandWrapper;
import dev.marulabs.commandlib.util.nms.command.NMSCommandDispatcher;
import dev.marulabs.commandlib.util.nms.command.NMSVanillaCommandWrapper;
import dev.marulabs.commandlib.util.nms.server.NMSCraftServer;
import dev.marulabs.commandlib.util.nms.server.NMSDedicatedServer;
import net.bytebuddy.ByteBuddy;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.help.HelpMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

// A registration on the vanilla dispatcher of a Spigot server, whose NMS classes are mocked.
@SuppressWarnings({"unchecked", "rawtypes"})
class CommandLibRegistrationTest {
    private final Server server = mock(Server.class);
    private final Plugin plugin = mock(Plugin.class);
    private final PluginManager pluginManager = mock(PluginManager.class);
    private final BukkitScheduler scheduler = mock(BukkitScheduler.class);
    private final NMSDedicatedServer dedicatedServer = mock(NMSDedicatedServer.class);
    // The dispatcher players see, and the vanilla one that "execute run" and functions use.
    private final RootCommandNode<Object> root = new RootCommandNode<>();
    private final RootCommandNode<Object> vanillaRoot = new RootCommandNode<>();
    private final SimpleCommandMap commandMap = new SimpleCommandMap(server);
    private final List<BukkitTask> timers = new ArrayList<>();
    private final List<CommandLib> registrations = new ArrayList<>();
    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<NMSCraftServer> craftServers;
    private MockedStatic<NMSVanillaCommandWrapper> wrappers;

    @BeforeEach
    void setUp() {
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(plugin.getName()).thenReturn("Test");
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("Test"));
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getServer)
              .thenReturn(server);
        bukkit.when(Bukkit::getPluginManager)
              .thenReturn(pluginManager);
        bukkit.when(Bukkit::isPrimaryThread)
              .thenReturn(true);
        bukkit.when(Bukkit::getOnlinePlayers)
              .thenReturn(List.of());
        bukkit.when(Bukkit::getBukkitVersion)
              .thenReturn("1.16.5-R0.1-SNAPSHOT");
        bukkit.when(Bukkit::getScheduler)
              .thenReturn(scheduler);
        bukkit.when(Bukkit::getHelpMap)
              .thenReturn(mock(HelpMap.class));
        when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenReturn(mock(BukkitTask.class));
        when(scheduler.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong())).thenAnswer(x -> {
            BukkitTask task = mock(BukkitTask.class);
            timers.add(task);
            return task;
        });

        NMSCommandDispatcher dispatcher = mock(NMSCommandDispatcher.class);
        doReturn(new CommandDispatcher<>(root)).when(dispatcher)
                                               .getBrigadier();
        NMSCommandDispatcher vanillaDispatcher = mock(NMSCommandDispatcher.class);
        doReturn(new CommandDispatcher<>(vanillaRoot)).when(vanillaDispatcher)
                                                      .getBrigadier();
        when(dedicatedServer.getCommandDispatcher()).thenReturn(dispatcher);
        when(dedicatedServer.getVanillaCommandDispatcher()).thenReturn(vanillaDispatcher);
        when(dedicatedServer.getDataPackResourcesHandle()).thenReturn(new Object());
        NMSCraftServer craftServer = mock(NMSCraftServer.class);
        when(craftServer.getServer()).thenReturn(dedicatedServer);
        when(craftServer.getCommandMap()).thenReturn(commandMap);
        craftServers = mockStatic(NMSCraftServer.class, x -> craftServer);
        NMSVanillaCommandWrapper wrapperFactory = mock(NMSVanillaCommandWrapper.class);
        when(wrapperFactory.createInstance(any(),
                                           any())).thenAnswer(x -> new FakeVanillaCommandWrapper(x.getArgument(1)));
        wrappers = mockStatic(NMSVanillaCommandWrapper.class, x -> wrapperFactory);
    }

    @AfterEach
    void tearDown() {
        // The registrations and the function compiler task are static, so none is left for other tests.
        registrations.forEach(CommandLib::unregister);
        wrappers.close();
        craftServers.close();
        bukkit.close();
    }

    @Test
    void the_function_compiler_runs_only_while_a_registration_is_left() {
        CommandLib spawn = register("spawn");
        CommandLib home = register("home");
        assertThat(timers).hasSize(1);

        spawn.unregister();
        verify(timers.get(0), never()).cancel();

        home.unregister();
        verify(timers.get(0)).cancel();
        // No task is left to drop the functions that used the commands, so they are compiled at once.
        verify(dedicatedServer).recompileFunctions(any(), anyBoolean());

        register("spawn");
        assertThat(timers).hasSize(2);
    }

    @Test
    void unregister_leaves_the_labels_and_the_vanilla_node_a_later_registration_took() {
        CommandLib first = register("spawn");
        CommandNode<Object> firstNode = vanillaRoot.getChild("spawn");
        CommandLib second = register("spawn");
        CommandNode<Object> secondNode = vanillaRoot.getChild("spawn");
        org.bukkit.command.Command wrapper = commandMap.getCommand("spawn");

        first.unregister();

        assertThat(secondNode).isNotSameAs(firstNode);
        assertThat(vanillaRoot.getChild("spawn")).isSameAs(secondNode);
        assertThat(root.getChild("spawn")).isSameAs(secondNode);
        assertThat(root.getChild("test:spawn")).isNotNull();
        assertThat(commandMap.getCommand("spawn")).isSameAs(wrapper);
        assertThat(commandMap.getCommand("test:spawn")).isSameAs(wrapper);

        second.unregister();

        assertThat(vanillaRoot.getChild("spawn")).isNull();
        assertThat(root.getChild("spawn")).isNull();
        assertThat(root.getChild("test:spawn")).isNull();
        assertThat(commandMap.getCommand("spawn")).isNull();
        assertThat(commandMap.getCommand("test:spawn")).isNull();
    }

    @Test
    void unregister_leaves_a_node_that_replaced_the_command_in_the_vanilla_dispatcher() {
        CommandLib registration = register("spawn");
        LiteralCommandNode<Object> other = LiteralArgumentBuilder.literal("spawn")
                                                                 .build();
        CommandNodeChildren.replace(vanillaRoot, List.of("spawn"), List.of(other));

        registration.unregister();

        assertThat(vanillaRoot.getChild("spawn")).isSameAs(other);
        assertThat(root.getChild("spawn")).isNull();
        assertThat(commandMap.getCommand("spawn")).isNull();
    }

    @Test
    void the_spigot_artifact_refuses_folia_before_anything_is_registered() throws Exception {
        // CommandLib looks for Folia's RegionizedServer through its own class loader, which here also defines one.
        ClassLoader parent = CommandLib.class.getClassLoader();
        ClassLoader folia = new ClassLoader(parent) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                byte[] bytes;
                if (name.equals("io.papermc.paper.threadedregions.RegionizedServer")) {
                    bytes = new ByteBuddy().subclass(Object.class)
                                           .name(name)
                                           .make()
                                           .getBytes();
                } else if (name.equals(CommandLib.class.getName())) {
                    try (InputStream in = parent.getResourceAsStream(name.replace('.', '/') + ".class")) {
                        bytes = Objects.requireNonNull(in)
                                       .readAllBytes();
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }
                } else {
                    return super.loadClass(name, resolve);
                }
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    return loaded != null ? loaded : defineClass(name, bytes, 0, bytes.length);
                }
            }
        };
        // The constructor, since the other copy of the class is in another runtime package than the commands.
        Constructor<?> constructor = folia.loadClass(CommandLib.class.getName())
                                          .getDeclaredConstructor(Plugin.class, Collection.class, String.class);
        constructor.setAccessible(true);

        assertThatThrownBy(() -> constructor.newInstance(plugin,
                                                         List.of(),
                                                         "test")).hasCauseInstanceOf(UnsupportedOperationException.class);
        bukkit.verify(Bukkit::isPrimaryThread, never());
        verifyNoInteractions(pluginManager);
    }

    private CommandLib register(String name) {
        CommandLib registration = CommandLib.register(plugin, new Command(name) {{
            execute(ctx -> {
            });
        }});
        registrations.add(registration);
        return registration;
    }
}
