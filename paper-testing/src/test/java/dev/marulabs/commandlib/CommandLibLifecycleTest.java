package dev.marulabs.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import dev.marulabs.commandlib.argument.IntegerArgument;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEventType;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.help.HelpMap;
import org.bukkit.help.HelpTopic;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Duration;
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
    private final List<Runnable> scheduledTasks = new ArrayList<>();
    // What the plugin manager holds, by name, as Bukkit's does.
    private final Map<String, Permission> permissions = new HashMap<>();
    private MockedStatic<Bukkit> bukkit;
    private PluginManager pluginManager;
    private Plugin plugin;

    @BeforeEach
    void setUp() {
        pluginManager = mock(PluginManager.class);
        when(pluginManager.getPermission(anyString())).thenAnswer(x -> permissions.get(x.<String>getArgument(0)));
        doAnswer(x -> {
            Permission permission = x.getArgument(0);
            if (permissions.putIfAbsent(permission.getName(), permission) != null) {
                throw new IllegalArgumentException("The permission " + permission.getName() + " is already defined!");
            }
            return null;
        }).when(pluginManager)
          .addPermission(any(Permission.class));
        doAnswer(x -> permissions.remove(x.<Permission>getArgument(0)
                                          .getName())).when(pluginManager)
                                                      .removePermission(any(Permission.class));
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
        bukkit.when(Bukkit::isPrimaryThread)
              .thenReturn(true);
        bukkit.when(Bukkit::getPluginManager)
              .thenReturn(pluginManager);
        bukkit.when(Bukkit::getOnlinePlayers)
              .thenReturn(List.of());
        // Permission#setDefault recalculates permissibles through the server.
        bukkit.when(Bukkit::getServer)
              .thenReturn(server);
        GlobalRegionScheduler scheduler = mock(GlobalRegionScheduler.class);
        doAnswer(invocation -> scheduledTasks.add(invocation.getArgument(1))).when(scheduler)
                                                                             .execute(any(Plugin.class),
                                                                                      any(Runnable.class));
        bukkit.when(Bukkit::getGlobalRegionScheduler)
              .thenReturn(scheduler);
        bukkit.when(Bukkit::getHelpMap)
              .thenReturn(mock(HelpMap.class));
        bukkit.when(Bukkit::getCommandMap)
              .thenReturn(mock(CommandMap.class));
    }

    @AfterEach
    void tearDown() {
        // CommandLib registers its listeners on the static handler lists, so remove them to keep tests isolated.
        HandlerList.unregisterAll(plugin);
        bukkit.close();
    }

    @Test
    void registration_and_unregistration_off_the_main_thread_are_refused() {
        CommandLib lib = CommandLib.register(plugin, "test", helloCommand());
        bukkit.when(Bukkit::isPrimaryThread)
              .thenReturn(false);

        assertThatThrownBy(() -> CommandLib.register(plugin,
                                                     "test",
                                                     namedCommand("other"))).isInstanceOf(IllegalStateException.class)
                                                                            .hasMessageContaining("main server thread");
        assertThatThrownBy(lib::unregister).isInstanceOf(IllegalStateException.class)
                                           .hasMessageContaining("main server thread");
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
    void plugin_enabled_after_startup_adds_its_commands_to_the_live_dispatcher_without_reloading_data() {
        when(plugin.isEnabled()).thenReturn(true);
        CommandLib.register(plugin, "test", helloCommand());
        CommandDispatcher<CommandSourceStack> dispatcher = fireCommandsEvent();
        runScheduledTasks();

        // Enabled by a plugin manager such as PlugMan after the COMMANDS event fired: Paper takes its handler, which
        // runs only on the next reload.
        Plugin enabledLater = pluginWithUnfiredHandler();
        try {
            CommandLib.register(enabledLater, "later", namedCommand("later"));
            CommandLib.register(enabledLater, "later", namedCommand("other"));
            when(enabledLater.isEnabled()).thenReturn(true);
            callEvent(new PluginEnableEvent(enabledLater));
            runScheduledTasks();

            bukkit.verify(Bukkit::reloadData, never());
            assertThat(dispatcher.getRoot()
                                 .getChild("later")).isNotNull();
            assertThat(dispatcher.getRoot()
                                 .getChild("enabledlater:later")).isNotNull();
            assertThat(dispatcher.getRoot()
                                 .getChild("other")).isNotNull();
        } finally {
            HandlerList.unregisterAll(enabledLater);
        }
    }

    @Test
    void commands_event_fired_at_startup_does_not_reload_data() {
        when(plugin.isEnabled()).thenReturn(false);
        CommandLib.register(plugin, "test", helloCommand());
        callEvent(new PluginEnableEvent(plugin));
        fireCommandsEvent();

        runScheduledTasks();

        bukkit.verify(Bukkit::reloadData, never());
    }

    @Test
    void unregistered_commands_do_not_reload_data() {
        when(plugin.isEnabled()).thenReturn(true);
        CommandLib.register(plugin, "test", helloCommand())
                  .unregister();

        runScheduledTasks();

        bukkit.verify(Bukkit::reloadData, never());
    }

    @Test
    void runtime_changes_update_command_trees_once_on_the_next_tick_without_reloading_data() {
        when(plugin.isEnabled()).thenReturn(true);
        Player player = onlinePlayer();
        CommandLib.register(plugin, "test", helloCommand());
        CommandDispatcher<CommandSourceStack> dispatcher = fireCommandsEvent();

        CommandLib.register(plugin, "test", namedCommand("late"));
        CommandLib.register(plugin, "test", namedCommand("later"))
                  .unregister();
        verify(player, never()).updateCommands();

        runScheduledTasks();

        verify(player).updateCommands();
        bukkit.verify(Bukkit::reloadData, never());
        assertThat(dispatcher.getRoot()
                             .getChild("late")).isNotNull();
        assertThat(dispatcher.getRoot()
                             .getChild("testplugin:late")).isNotNull();
        assertThat(dispatcher.getRoot()
                             .getChild("later")).isNull();
    }

    @Test
    void plugin_disable_updates_command_trees_at_once() {
        when(plugin.isEnabled()).thenReturn(true);
        Player player = onlinePlayer();
        CommandLib.register(plugin, "test", helloCommand());
        fireCommandsEvent();

        callDisableEvent(plugin);

        verify(player).updateCommands();
    }

    @Test
    void help_topics_are_restricted_to_the_command_permission_once_the_help_map_is_built() {
        when(plugin.isEnabled()).thenReturn(true);
        CommandLib.register(plugin, "test", new Command("hello") {{
            addAliases("hi");
            execute(ctx -> {
            });
        }});
        fireCommandsEvent();
        HelpMap helpMap = mock(HelpMap.class);
        HelpTopic hello = mock(HelpTopic.class);
        HelpTopic namespacedHi = mock(HelpTopic.class);
        when(hello.getName()).thenReturn("/hello");
        when(namespacedHi.getName()).thenReturn("/testplugin:hi");
        when(helpMap.getHelpTopic("/hello")).thenReturn(hello);
        when(helpMap.getHelpTopic("/testplugin:hi")).thenReturn(namespacedHi);
        bukkit.when(Bukkit::getHelpMap)
              .thenReturn(helpMap);

        callEvent(new ServerLoadEvent(ServerLoadEvent.LoadType.STARTUP));
        callEvent(new ServerResourcesReloadedEvent(ServerResourcesReloadedEvent.Cause.COMMAND));

        verify(hello, times(2)).amendCanSee("test.hello");
        verify(namespacedHi, times(2)).amendCanSee("test.hello");
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
    void a_cooldown_bypass_node_that_exists_is_neither_replaced_nor_removed() {
        when(plugin.isEnabled()).thenReturn(true);
        Permission declared = new Permission("myplugin.admin");
        permissions.put("myplugin.admin", declared);
        Command command = new Command("hello") {{
            cooldown(Cooldown.of(Duration.ofSeconds(5))
                             .bypassPermission("myplugin.admin"));
            execute(ctx -> {
            });
        }};

        CommandLib.register(plugin, "test", command)
                  .unregister();

        verify(pluginManager, never()).addPermission(argThat((Permission x) -> x.getName()
                                                                                .equals("myplugin.admin")));
        verify(pluginManager, never()).removePermission(declared);
        verify(pluginManager, times(1)).removePermission(any(Permission.class));
    }

    @Test
    void unregister_leaves_the_permission_a_later_registration_put_under_the_same_name() {
        when(plugin.isEnabled()).thenReturn(false);
        CommandLib first = CommandLib.register(plugin, "test", helloCommand());
        Permission older = permissions.get("test.hello");
        CommandLib second = CommandLib.register(plugin, "test", helloCommand());
        Permission newer = permissions.get("test.hello");

        first.unregister();

        assertThat(newer).isNotNull()
                         .isNotSameAs(older);
        assertThat(permissions.get("test.hello")).isSameAs(newer);
        assertThat(newer.getDefault()).isEqualTo(PermissionDefault.OP);

        second.unregister();

        assertThat(permissions).doesNotContainKey("test.hello");
    }

    @Test
    void unregistering_the_newer_of_two_registrations_of_a_node_gives_it_back_to_the_older_one() {
        when(plugin.isEnabled()).thenReturn(false);
        // A prefix of its own, as the registrations other tests leave in this copy of CommandLib are live as well.
        CommandLib older = CommandLib.register(plugin, "restore", new Command("hello") {{
            permission(PermissionDefault.TRUE);
            execute(ctx -> {
            });
        }});
        CommandLib newer = CommandLib.register(plugin, "restore", helloCommand());

        newer.unregister();

        assertThat(permissions.get("restore.hello")).isNotNull()
                                                    .extracting(Permission::getDefault)
                                                    .isEqualTo(PermissionDefault.TRUE);

        older.unregister();

        assertThat(permissions).doesNotContainKey("restore.hello");
    }

    @Test
    void a_first_registration_after_the_plugins_initialization_is_refused() {
        when(plugin.isEnabled()).thenReturn(true);
        CommandLib.register(plugin, "test", helloCommand());
        CommandDispatcher<CommandSourceStack> dispatcher = fireCommandsEvent();

        Plugin latePlugin = latePlugin();
        try {
            assertThatThrownBy(() -> CommandLib.register(latePlugin,
                                                         "late",
                                                         namedCommand("late"))).isInstanceOf(IllegalStateException.class)
                                                                               .hasMessageContaining("LatePlugin")
                                                                               .hasMessageContaining("initialization");
            assertThat(dispatcher.getRoot()
                                 .getChild("late")).isNull();
        } finally {
            HandlerList.unregisterAll(latePlugin);
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
    void unregister_leaves_a_label_that_a_later_registration_took() {
        when(plugin.isEnabled()).thenReturn(true);
        CommandLib first = CommandLib.register(plugin, "test", helloCommand());
        CommandDispatcher<CommandSourceStack> dispatcher = fireCommandsEvent();
        CommandLib.register(plugin, "test", helloCommand());
        CommandNode<CommandSourceStack> taken = dispatcher.getRoot()
                                                          .getChild("hello");
        CommandNode<CommandSourceStack> namespacedTaken = dispatcher.getRoot()
                                                                    .getChild("testplugin:hello");

        first.unregister();

        assertThat(dispatcher.getRoot()
                             .getChild("hello")).isNotNull()
                                                .isSameAs(taken);
        assertThat(dispatcher.getRoot()
                             .getChild("testplugin:hello")).isNotNull()
                                                           .isSameAs(namespacedTaken);
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
    void an_invalid_definition_throws_from_a_registration_during_initialization_before_anything_is_registered() {
        when(plugin.isEnabled()).thenReturn(false);
        Command broken = new Command("broken") {{
            argument(new IntegerArgument("count").optional(1),
                     new IntegerArgument("amount")).execute((count, amount, ctx) -> {
                     });
        }};

        assertThatThrownBy(() -> CommandLib.register(plugin, "test", broken)).isInstanceOf(IllegalStateException.class)
                                                                             .hasMessageContaining("optional argument 'count'");
        verify(pluginManager, never()).addPermission(any(Permission.class));
        assertThat(disableListeners()).isEmpty();
        assertThat(handlers).isEmpty();
    }

    @Test
    void failed_runtime_registration_is_cleaned_up_and_not_restored_by_reload() {
        when(plugin.isEnabled()).thenReturn(true);
        CommandLib.register(plugin, "test", helloCommand());
        RejectingRoot root = new RejectingRoot("broken");
        fireCommandsEvent(new CommandDispatcher<>(root));

        assertThatThrownBy(() -> CommandLib.register(plugin,
                                                     "test",
                                                     namedCommand("broken"))).isInstanceOf(IllegalStateException.class);
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

    // Reads back the server that setUp stubbed on the plugin mock.
    @SuppressWarnings("DirectInvocationOnMock")
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

    // Reads back the server that setUp stubbed on the plugin mock.
    @SuppressWarnings("DirectInvocationOnMock")
    private Plugin pluginWithUnfiredHandler() {
        Server server = plugin.getServer();
        Plugin other = mock(Plugin.class);
        when(other.getName()).thenReturn("EnabledLater");
        when(other.getLogger()).thenReturn(Logger.getLogger("EnabledLater"));
        when(other.getServer()).thenReturn(server);
        when(other.getLifecycleManager()).thenReturn(mock(LifecycleEventManager.class));
        return other;
    }

    private Player onlinePlayer() {
        Player player = mock(Player.class);
        bukkit.when(Bukkit::getOnlinePlayers)
              .thenReturn(List.of(player));
        return player;
    }

    private List<RegisteredListener> disableListeners() {
        return Arrays.stream(PluginDisableEvent.getHandlerList()
                                               .getRegisteredListeners())
                     .filter(x -> x.getPlugin() == plugin)
                     .toList();
    }

    private void runScheduledTasks() {
        List<Runnable> tasks = new ArrayList<>(scheduledTasks);
        scheduledTasks.clear();
        tasks.forEach(Runnable::run);
    }

    private static void callDisableEvent(Plugin disabled) {
        callEvent(new PluginDisableEvent(disabled));
    }

    private static void callEvent(Event event) {
        for (RegisteredListener listener : event.getHandlers()
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
            dispatcher.getRoot()
                      .addChild(PaperDispatcher.renamed(node, "testplugin:" + node.getLiteral()));
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
