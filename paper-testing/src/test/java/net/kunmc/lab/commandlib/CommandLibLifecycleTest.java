package net.kunmc.lab.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEventType;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "UnstableApiUsage"})
class CommandLibLifecycleTest {
    private final List<LifecycleEventHandler<ReloadableRegistrarEvent<Commands>>> handlers = new ArrayList<>();
    private MockedStatic<Bukkit> bukkit;
    private PluginManager pluginManager;
    private Plugin plugin;

    @BeforeEach
    void setUp() {
        pluginManager = mock(PluginManager.class);
        Server server = mock(Server.class);
        when(server.getPluginManager()).thenReturn(pluginManager);

        LifecycleEventManager<Plugin> lifecycleManager = mock(LifecycleEventManager.class);
        doAnswer(invocation -> {
            handlers.add(invocation.getArgument(1));
            return null;
        }).when(lifecycleManager)
          .registerEventHandler(any(LifecycleEventType.class), any(LifecycleEventHandler.class));

        plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("TestPlugin");
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getLifecycleManager()).thenReturn(lifecycleManager);

        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager)
              .thenReturn(pluginManager);
        bukkit.when(Bukkit::getOnlinePlayers)
              .thenReturn(List.of());
        // Permission#setDefault recalculates permissibles through the server.
        bukkit.when(Bukkit::getServer)
              .thenReturn(server);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void registration_before_enable_is_deferred_to_commands_event() {
        when(plugin.isEnabled()).thenReturn(false);

        CommandLib.register(plugin, "test", helloCommand());

        verify(pluginManager, never()).registerEvents(any(Listener.class), any(Plugin.class));
        verify(pluginManager).addPermission(any(Permission.class));

        CommandDispatcher<CommandSourceStack> dispatcher = fireCommandsEvent();

        assertThat(dispatcher.getRoot()
                             .getChild("hello")).isNotNull();
    }

    @Test
    void disable_listener_is_registered_once_plugin_is_enabled() {
        when(plugin.isEnabled()).thenReturn(false);
        CommandLib.register(plugin, "test", helloCommand());

        when(plugin.isEnabled()).thenReturn(true);
        fireCommandsEvent();
        fireCommandsEvent();

        verify(pluginManager, times(1)).registerEvents(any(Listener.class), eq(plugin));
    }

    @Test
    void registration_while_enabled_registers_disable_listener_immediately() {
        when(plugin.isEnabled()).thenReturn(true);

        CommandLib.register(plugin, "test", helloCommand());

        verify(pluginManager, times(1)).registerEvents(any(Listener.class), eq(plugin));
    }

    @Test
    void commands_are_registered_again_after_reload() {
        when(plugin.isEnabled()).thenReturn(true);
        CommandLib.register(plugin, "test", helloCommand());

        CommandDispatcher<CommandSourceStack> first = fireCommandsEvent();
        CommandDispatcher<CommandSourceStack> reloaded = fireCommandsEvent();

        assertThat(first.getRoot()
                        .getChild("hello")).isNotNull();
        assertThat(reloaded.getRoot()
                           .getChild("hello")).isNotNull();
    }

    @Test
    void registration_after_commands_event_is_kept_across_reload() {
        when(plugin.isEnabled()).thenReturn(true);
        CommandLib.register(plugin, "test", helloCommand());
        CommandDispatcher<CommandSourceStack> first = fireCommandsEvent();

        CommandLib.register(plugin, "test", namedCommand("late"));
        CommandDispatcher<CommandSourceStack> reloaded = fireCommandsEvent();

        assertThat(first.getRoot()
                        .getChild("late")).isNotNull();
        assertThat(reloaded.getRoot()
                           .getChild("late")).isNotNull();
    }

    @Test
    void unregister_removes_commands_and_is_not_restored_by_reload() {
        when(plugin.isEnabled()).thenReturn(true);
        CommandLib lib = CommandLib.register(plugin, "test", helloCommand());
        CommandDispatcher<CommandSourceStack> dispatcher = fireCommandsEvent();
        dispatcher.getRoot()
                  .addChild(Commands.literal("testplugin:hello")
                                    .build());

        lib.unregister();
        lib.unregister();
        CommandDispatcher<CommandSourceStack> reloaded = fireCommandsEvent();

        assertThat(dispatcher.getRoot()
                             .getChild("hello")).isNull();
        assertThat(dispatcher.getRoot()
                             .getChild("testplugin:hello")).isNull();
        assertThat(reloaded.getRoot()
                           .getChild("hello")).isNull();
        verify(pluginManager, times(1)).removePermission(any(Permission.class));
    }

    @Test
    void unregister_after_reload_removes_commands_from_current_dispatcher() {
        when(plugin.isEnabled()).thenReturn(true);
        CommandLib lib = CommandLib.register(plugin, "test", helloCommand());
        fireCommandsEvent();
        CommandDispatcher<CommandSourceStack> reloaded = fireCommandsEvent();

        lib.unregister();

        assertThat(reloaded.getRoot()
                           .getChild("hello")).isNull();
    }

    @Test
    void unregister_uses_root_remove_command_when_available() {
        when(plugin.isEnabled()).thenReturn(true);
        CommandLib lib = CommandLib.register(plugin, "test", helloCommand());
        MirrorRoot root = new MirrorRoot();
        fireCommandsEvent(new CommandDispatcher<>(root));

        lib.unregister();

        assertThat(root.removed).containsExactly("hello", "testplugin:hello");
    }

    /**
     * Stands in for Paper's ApiMirrorRootNode, whose removeCommand also removes the node from the vanilla dispatcher.
     */
    public static final class MirrorRoot extends RootCommandNode<CommandSourceStack> {
        private final List<String> removed = new ArrayList<>();

        public void removeCommand(String name) {
            removed.add(name);
        }
    }

    private CommandDispatcher<CommandSourceStack> fireCommandsEvent() {
        return fireCommandsEvent(new CommandDispatcher<>());
    }

    private CommandDispatcher<CommandSourceStack> fireCommandsEvent(CommandDispatcher<CommandSourceStack> dispatcher) {
        Commands registrar = mock(Commands.class);
        when(registrar.getDispatcher()).thenReturn(dispatcher);
        when(registrar.register(any(LiteralCommandNode.class), anyString())).thenAnswer(invocation -> {
            dispatcher.getRoot()
                      .addChild(invocation.getArgument(0));
            return Set.of();
        });
        ReloadableRegistrarEvent<Commands> event = mock(ReloadableRegistrarEvent.class);
        when(event.registrar()).thenReturn(registrar);

        assertThat(handlers).hasSize(1);
        handlers.get(0)
                .run(event);
        return dispatcher;
    }

    private static Command helloCommand() {
        return namedCommand("hello");
    }

    private static Command namedCommand(String name) {
        return new Command(name) {{
            execute(ctx -> ctx.sendMessage(name));
        }};
    }
}
