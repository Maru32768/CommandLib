package net.kunmc.lab.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
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
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.*;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "UnstableApiUsage"})
class CommandLibLifecycleTest {
    private final List<LifecycleEventHandler<ReloadableRegistrarEvent<Commands>>> handlers = new ArrayList<>();
    private final Set<String> foreignLabels = new HashSet<>();
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
        when(plugin.getLogger()).thenReturn(Logger.getLogger("TestPlugin"));
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
        // CommandLib registers its disable listener on the static handler list, so remove it to keep tests isolated.
        PluginDisableEvent.getHandlerList()
                          .unregister(plugin);
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
    void disable_listener_is_registered_on_handler_list_before_enable() {
        when(plugin.isEnabled()).thenReturn(false);

        CommandLib.register(plugin, "test", helloCommand());

        assertThat(disableListeners()).hasSize(1);
    }

    @Test
    void plugin_disable_event_unregisters_commands_and_listener() {
        when(plugin.isEnabled()).thenReturn(true);
        CommandLib.register(plugin, "test", helloCommand());
        CommandDispatcher<CommandSourceStack> dispatcher = fireCommandsEvent();

        callDisableEvent(mock(Plugin.class));
        assertThat(dispatcher.getRoot()
                             .getChild("hello")).isNotNull();

        callDisableEvent(plugin);

        assertThat(dispatcher.getRoot()
                             .getChild("hello")).isNull();
        verify(pluginManager, times(1)).removePermission(any(Permission.class));
        assertThat(disableListeners()).isEmpty();
    }

    @Test
    void fallback_registration_is_restored_after_reload() {
        when(plugin.isEnabled()).thenReturn(true);
        CommandLib.register(plugin, "test", helloCommand());
        CommandDispatcher<CommandSourceStack> first = fireCommandsEvent();

        Plugin latePlugin = latePlugin();
        try {
            CommandLib.register(latePlugin, "late", namedCommand("late"));
            CommandDispatcher<CommandSourceStack> reloaded = fireCommandsEvent();
            CommandLib.register(latePlugin, "late", namedCommand("later"));

            assertThat(first.getRoot()
                            .getChild("late")).isNotNull();
            assertThat(reloaded.getRoot()
                               .getChild("late")).isNotNull();
            assertThat(reloaded.getRoot()
                               .getChild("later")).isNotNull();
        } finally {
            PluginDisableEvent.getHandlerList()
                              .unregister(latePlugin);
        }
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

        assertThat(root.removed).containsExactlyInAnyOrder("hello", "testplugin:hello");
    }

    @Test
    void unregister_leaves_labels_paper_did_not_register_for_this_plugin() {
        when(plugin.isEnabled()).thenReturn(true);
        foreignLabels.add("hello");
        CommandLib lib = CommandLib.register(plugin, "test", helloCommand());
        MirrorRoot root = new MirrorRoot();
        fireCommandsEvent(new CommandDispatcher<>(root));

        lib.unregister();

        assertThat(root.removed).containsExactly("testplugin:hello");
    }

    @Test
    void unregister_continues_after_a_command_fails_to_be_removed() {
        when(plugin.isEnabled()).thenReturn(true);
        CommandLib lib = CommandLib.register(plugin, "test", helloCommand());
        MirrorRoot root = new MirrorRoot();
        root.failing.add("hello");
        fireCommandsEvent(new CommandDispatcher<>(root));

        lib.unregister();

        assertThat(root.removed).containsExactly("testplugin:hello");
        verify(pluginManager, times(1)).removePermission(any(Permission.class));
        assertThat(disableListeners()).isEmpty();
    }

    @Test
    void duplicate_top_level_names_are_rejected_before_anything_is_registered() {
        when(plugin.isEnabled()).thenReturn(true);

        assertThatThrownBy(() -> CommandLib.register(plugin, "test", helloCommand(), new Command("hi") {{
            addAliases("hello");
        }})).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("hello");
        verify(pluginManager, never()).addPermission(any(Permission.class));
        assertThat(disableListeners()).isEmpty();
    }

    @Test
    void failed_runtime_registration_is_cleaned_up_and_not_restored_by_reload() {
        when(plugin.isEnabled()).thenReturn(true);
        CommandLib.register(plugin, "test", helloCommand());
        RejectingRoot root = new RejectingRoot("broken");
        fireCommandsEvent(new CommandDispatcher<>(root));

        assertThatThrownBy(() -> CommandLib.register(plugin, "test", namedCommand("broken"))).isInstanceOf(
                IllegalStateException.class);
        verify(pluginManager, times(1)).removePermission(any(Permission.class));

        CommandDispatcher<CommandSourceStack> reloaded = fireCommandsEvent();

        assertThat(reloaded.getRoot()
                           .getChild("hello")).isNotNull();
        assertThat(reloaded.getRoot()
                           .getChild("broken")).isNull();
    }

    @Test
    void failing_registration_does_not_block_others_in_commands_event() {
        when(plugin.isEnabled()).thenReturn(false);
        CommandLib.register(plugin, "test", namedCommand("broken"));
        CommandLib.register(plugin, "test", helloCommand());
        RejectingRoot root = new RejectingRoot("broken");

        CommandDispatcher<CommandSourceStack> dispatcher = fireCommandsEvent(new CommandDispatcher<>(root));

        assertThat(dispatcher.getRoot()
                             .getChild("hello")).isNotNull();
    }

    public static final class RejectingRoot extends RootCommandNode<CommandSourceStack> {
        private final String rejected;

        public RejectingRoot(String rejected) {
            this.rejected = rejected;
        }

        @Override
        public void addChild(CommandNode<CommandSourceStack> node) {
            if (node.getName()
                    .equals(rejected)) {
                throw new IllegalStateException("rejected " + rejected);
            }
            super.addChild(node);
        }
    }

    /**
     * Stands in for Paper's ApiMirrorRootNode, whose removeCommand also removes the node from the vanilla dispatcher.
     */
    public static final class MirrorRoot extends RootCommandNode<CommandSourceStack> {
        private final List<String> removed = new ArrayList<>();
        private final Set<String> failing = new HashSet<>();

        public void removeCommand(String name) {
            if (failing.contains(name)) {
                throw new IllegalStateException("cannot remove " + name);
            }
            removed.add(name);
        }
    }

    private Plugin latePlugin() {
        LifecycleEventManager<Plugin> lifecycleManager = mock(LifecycleEventManager.class);
        doThrow(new IllegalStateException("closed")).when(lifecycleManager)
                                                    .registerEventHandler(any(LifecycleEventType.class),
                                                                          any(LifecycleEventHandler.class));
        Server server = plugin.getServer();
        Plugin latePlugin = mock(Plugin.class);
        when(latePlugin.getName()).thenReturn("LatePlugin");
        when(latePlugin.getServer()).thenReturn(server);
        when(latePlugin.getLifecycleManager()).thenReturn(lifecycleManager);
        when(latePlugin.isEnabled()).thenReturn(true);
        return latePlugin;
    }

    private List<RegisteredListener> disableListeners() {
        return Arrays.stream(PluginDisableEvent.getHandlerList()
                                               .getRegisteredListeners())
                     .filter(x -> x.getPlugin() == plugin)
                     .toList();
    }

    private static void callDisableEvent(Plugin disabled) {
        PluginDisableEvent event = new PluginDisableEvent(disabled);
        for (RegisteredListener listener : PluginDisableEvent.getHandlerList()
                                                             .getRegisteredListeners()) {
            try {
                listener.callEvent(event);
            } catch (EventException e) {
                throw new AssertionError(e);
            }
        }
    }

    private CommandDispatcher<CommandSourceStack> fireCommandsEvent() {
        return fireCommandsEvent(new CommandDispatcher<>());
    }

    private CommandDispatcher<CommandSourceStack> fireCommandsEvent(CommandDispatcher<CommandSourceStack> dispatcher) {
        Commands registrar = mock(Commands.class);
        when(registrar.getDispatcher()).thenReturn(dispatcher);
        when(registrar.register(any(LiteralCommandNode.class), anyString())).thenAnswer(invocation -> {
            LiteralCommandNode<CommandSourceStack> node = invocation.getArgument(0);
            dispatcher.getRoot()
                      .addChild(node);
            // Like Paper, report the plain and namespaced labels unless the plain one belongs to another plugin.
            if (foreignLabels.contains(node.getLiteral())) {
                return Set.of("testplugin:" + node.getLiteral());
            }
            return Set.of(node.getLiteral(), "testplugin:" + node.getLiteral());
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
