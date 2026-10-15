package dev.marulabs.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.ProxyVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandLibTest {
    private final Object plugin = new Object();
    private final ProxyServer proxy = Mockito.mock(ProxyServer.class);
    private final CommandManager commandManager = Mockito.mock(CommandManager.class);
    private final ArgumentCaptor<BrigadierCommand> registered = ArgumentCaptor.forClass(BrigadierCommand.class);

    @BeforeEach
    void setUp() {
        PluginManager pluginManager = Mockito.mock(PluginManager.class);
        PluginContainer container = Mockito.mock(PluginContainer.class);
        PluginDescription description = Mockito.mock(PluginDescription.class);
        Mockito.when(description.getId())
               .thenReturn("myplugin");
        Mockito.when(container.getDescription())
               .thenReturn(description);
        Mockito.when(pluginManager.fromInstance(plugin))
               .thenReturn(Optional.of(container));
        Mockito.when(proxy.getPluginManager())
               .thenReturn(pluginManager);
        Mockito.when(proxy.getCommandManager())
               .thenReturn(commandManager);
        Mockito.when(proxy.getVersion())
               .thenReturn(new ProxyVersion("Velocity", "Velocity Contributors", "3.4.0"));
        Mockito.when(commandManager.metaBuilder(Mockito.any(BrigadierCommand.class)))
               .thenAnswer(invocation -> {
                   CommandMeta.Builder builder = Mockito.mock(CommandMeta.Builder.class);
                   Mockito.when(builder.plugin(Mockito.any()))
                          .thenReturn(builder);
                   Mockito.when(builder.build())
                          .thenReturn(Mockito.mock(CommandMeta.class));
                   return builder;
               });
    }

    @Test
    void registers_each_command_and_alias_and_unregisters_them() {
        CommandLib commandLib = CommandLib.register(plugin, proxy, new Command("hub") {{
            addAliases("lobby");
            execute(ctx -> ctx.sendMessage("hub"));
        }});

        Mockito.verify(commandManager, Mockito.times(2))
               .register(Mockito.any(CommandMeta.class), registered.capture());
        assertThat(registered.getAllValues()
                             .stream()
                             .map(x -> x.getNode()
                                        .getLiteral())
                             .collect(Collectors.toList())).containsExactly("hub", "lobby");

        assertThat(CommandLib.diagnostics()
                             .registrations()).anySatisfy(x -> {
                                 assertThat(x.owner()).isEqualTo("myplugin");
                                 assertThat(x.labels()).containsExactly("hub", "lobby");
                             });

        commandLib.unregister();
        Mockito.verify(commandManager, Mockito.times(2))
               .unregister(Mockito.any(CommandMeta.class));
        assertThat(CommandLib.diagnostics()
                             .registrations()).noneSatisfy(x -> assertThat(x.labels()).containsExactly("hub", "lobby"));
    }

    @Test
    void registration_defaults_apply_to_every_command() throws CommandSyntaxException {
        List<String> log = new java.util.ArrayList<>();
        CommandDefaults<CommandContext> defaults = CommandDefaults.of(CommandContext.class)
                                                                  .addMiddleware((ctx, next) -> {
                                                                      log.add("audit");
                                                                      return next.proceed();
                                                                  })
                                                                  .provide(String.class, ctx -> "default");
        CommandLib.register(plugin, proxy, defaults, List.of(new Command("hub") {{
            permission(DefaultPermission.ALL);
            execute(ctx -> log.add("hub " + ctx.get(String.class)));
        }}));
        Mockito.verify(commandManager)
               .register(Mockito.any(CommandMeta.class), registered.capture());
        CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();
        dispatcher.getRoot()
                  .addChild(registered.getValue()
                                      .getNode());
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getPermissionValue(Mockito.anyString()))
               .thenReturn(Tristate.UNDEFINED);

        dispatcher.execute("hub", player);

        assertThat(log).containsExactly("audit", "hub default");
    }

    @Test
    void permission_defaults_apply_when_the_permission_is_unset() throws CommandSyntaxException {
        CommandLib.register(plugin, proxy, new Command("hub") {{
            permission(DefaultPermission.ALL);
            execute(ctx -> ctx.sendMessage("hub"));
        }}, new Command("kick") {{
            execute(ctx -> ctx.sendMessage("kick"));
        }});
        Mockito.verify(commandManager, Mockito.times(2))
               .register(Mockito.any(CommandMeta.class), registered.capture());
        CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();
        registered.getAllValues()
                  .forEach(x -> dispatcher.getRoot()
                                          .addChild(x.getNode()));
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getPermissionValue(Mockito.anyString()))
               .thenReturn(Tristate.UNDEFINED);

        dispatcher.execute("hub", player);
        assertThatThrownBy(() -> dispatcher.execute("kick", player)).isInstanceOf(CommandSyntaxException.class);

        Mockito.when(player.getPermissionValue("myplugin.command.kick"))
               .thenReturn(Tristate.TRUE);
        Mockito.when(player.getPermissionValue("myplugin.command.hub"))
               .thenReturn(Tristate.FALSE);
        dispatcher.execute("kick", player);
        assertThatThrownBy(() -> dispatcher.execute("hub", player)).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void rejects_an_object_that_is_not_a_plugin() {
        assertThatThrownBy(() -> CommandLib.register(new Object(),
                                                     proxy,
                                                     List.of(new Command("hub")))).isInstanceOf(IllegalArgumentException.class);
    }
}
