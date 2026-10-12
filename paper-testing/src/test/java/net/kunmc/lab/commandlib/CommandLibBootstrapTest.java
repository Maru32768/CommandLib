package net.kunmc.lab.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEventType;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "UnstableApiUsage"})
class CommandLibBootstrapTest {
    private final List<LifecycleEventHandler<ReloadableRegistrarEvent<Commands>>> handlers = new ArrayList<>();
    private MockedStatic<Bukkit> bukkit;
    private PluginManager pluginManager;
    private BootstrapContext context;

    @BeforeEach
    void setUp() {
        LifecycleEventManager<BootstrapContext> lifecycleManager = mock(LifecycleEventManager.class);
        doAnswer(invocation -> handlers.add(invocation.getArgument(1))).when(lifecycleManager)
                                                                       .registerEventHandler(any(LifecycleEventType.class),
                                                                                             any(LifecycleEventHandler.class));
        PluginMeta meta = mock(PluginMeta.class);
        when(meta.getName()).thenReturn("BootPlugin");
        context = mock(BootstrapContext.class);
        when(context.getPluginMeta()).thenReturn(meta);
        when(context.getLifecycleManager()).thenReturn(lifecycleManager);

        pluginManager = mock(PluginManager.class);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager)
              .thenReturn(pluginManager);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void commands_are_registered_on_every_commands_event() {
        CommandLib.bootstrap(context, namedCommand("boothello"));

        assertThat(fireCommandsEvent().getRoot()
                                      .getChild("boothello")).isNotNull();
        assertThat(fireCommandsEvent().getRoot()
                                      .getChild("boothello")).isNotNull();
    }

    @Test
    void unregistered_permissions_use_the_command_defaults() {
        bukkit.when(Bukkit::getServer)
              .thenReturn(mock(Server.class));
        Command everyone = namedCommand("booteveryone");
        everyone.permission(PermissionDefault.TRUE);
        Command nobody = namedCommand("bootnobody");
        nobody.permission(PermissionDefault.FALSE);
        CommandLib.bootstrap(context, everyone, nobody);
        CommandDispatcher<CommandSourceStack> dispatcher = fireCommandsEvent();

        assertThat(canUse(dispatcher, "booteveryone", player(false))).isTrue();
        assertThat(canUse(dispatcher, "bootnobody", player(true))).isFalse();
    }

    @Test
    void registered_or_set_permissions_are_checked_by_the_sender() {
        bukkit.when(Bukkit::getServer)
              .thenReturn(mock(Server.class));
        Command nobody = namedCommand("bootregistered");
        nobody.permission(PermissionDefault.FALSE);
        CommandLib.bootstrap(context, nobody);
        CommandDispatcher<CommandSourceStack> dispatcher = fireCommandsEvent();
        Player op = player(true);
        when(op.isPermissionSet("bootplugin.command.bootregistered")).thenReturn(true);

        assertThat(canUse(dispatcher, "bootregistered", op)).isTrue();
    }

    @Test
    void permissions_are_checked_by_the_sender_before_the_server_exists() {
        Command nobody = namedCommand("bootearly");
        nobody.permission(PermissionDefault.FALSE);
        CommandLib.bootstrap(context, nobody);

        assertThat(canUse(fireCommandsEvent(), "bootearly", player(true))).isTrue();
    }

    private static boolean canUse(CommandDispatcher<CommandSourceStack> dispatcher, String name, Player sender) {
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.getSender()).thenReturn(sender);
        CommandNode<CommandSourceStack> node = dispatcher.getRoot()
                                                         .getChild(name);
        return node.canUse(source);
    }

    private static Player player(boolean op) {
        Player player = mock(Player.class);
        when(player.isOp()).thenReturn(op);
        // Like Bukkit for a permission that is neither set nor registered.
        when(player.hasPermission(anyString())).thenReturn(op);
        return player;
    }

    private CommandDispatcher<CommandSourceStack> fireCommandsEvent() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        Commands registrar = mock(Commands.class);
        when(registrar.register(any(LiteralCommandNode.class), anyString())).thenAnswer(invocation -> {
            dispatcher.getRoot()
                      .addChild(invocation.getArgument(0));
            return Set.of();
        });
        ReloadableRegistrarEvent<Commands> event = mock(ReloadableRegistrarEvent.class);
        when(event.registrar()).thenReturn(registrar);
        handlers.forEach(handler -> handler.run(event));
        return dispatcher;
    }

    private static Command namedCommand(String name) {
        return new Command(name) {{
            execute(ctx -> {
            });
        }};
    }
}
