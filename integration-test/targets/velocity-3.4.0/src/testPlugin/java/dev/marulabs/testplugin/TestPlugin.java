package dev.marulabs.testplugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.marulabs.cadence.Cadence;
import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.CommonArgument;
import dev.marulabs.cadence.DefaultPermission;
import dev.marulabs.cadence.argument.BooleanArgument;
import dev.marulabs.cadence.argument.DoubleArgument;
import dev.marulabs.cadence.argument.DurationArgument;
import dev.marulabs.cadence.argument.EnumArgument;
import dev.marulabs.cadence.argument.FloatArgument;
import dev.marulabs.cadence.argument.IntegerArgument;
import dev.marulabs.cadence.argument.LongArgument;
import dev.marulabs.cadence.argument.Nameable;
import dev.marulabs.cadence.argument.NameableObjectArgument;
import dev.marulabs.cadence.argument.ObjectArgument;
import dev.marulabs.cadence.argument.PlayerArgument;
import dev.marulabs.cadence.argument.ServerArgument;
import dev.marulabs.cadence.argument.StringArgument;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The commands VelocityIntegrationTest runs from the bot, which joins through the proxy. Velocity has no operators,
 * and no permission plugin is installed, so only the nodes with {@link DefaultPermission#ALL} are the player's.
 */
@Plugin(id = "cadencetest", name = "CadenceTest", version = "1.0.0")
public final class TestPlugin {
    private final ProxyServer proxy;

    @Inject
    public TestPlugin(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        Cadence.register(this, proxy, command());
        // The console has every permission, so this shows that the OP node the player is refused is registered.
        proxy.getCommandManager()
             .executeAsync(proxy.getConsoleCommandSource(), "cadencetest denied");
    }

    private static Command command() {
        Command main = new Command("cadencetest") {{
            permission(DefaultPermission.ALL);
        }};
        main.addChildren(new Command("echo") {{
            permission(DefaultPermission.ALL);
            argument(new StringArgument("text")).execute((ctx, text) -> ctx.sendSuccess("echo " + text));
        }});
        main.addChildren(new Command("server") {{
            permission(DefaultPermission.ALL);
            argument(new ServerArgument("target")).execute((ctx,
                                                            target) -> ctx.sendSuccess("server "
                                                                    + target.getServerInfo()
                                                                            .getName()));
        }});
        main.addChildren(new Command("consoleOnly") {{
            permission(DefaultPermission.ALL);
            addPrerequisite(Command.console());
            execute(ctx -> ctx.sendSuccess("consoleOnly executed"));
        }});
        main.addChildren(new Command("find") {{
            permission(DefaultPermission.ALL);
            argument(new PlayerArgument("player")).execute((ctx, player) -> ctx.sendSuccess("find "
                    + player.getUsername()));
        }});
        main.addChildren(new Command("denied") {{
            permission(DefaultPermission.OP);
            execute(ctx -> ctx.sendSuccess("denied executed"));
        }});
        main.addChildren(new Command("async") {{
            permission(DefaultPermission.ALL);
            executeAsync(ctx -> {
                Thread caller = Thread.currentThread();
                return ctx.supplyAsync(() -> Thread.currentThread() != caller)
                          .thenAccept(offCaller -> ctx.sendSuccess("async done, work off the calling thread: "
                                  + offCaller));
            });
        }});
        // One child of /cadencetest value per argument type, which sends the parsed value.
        main.addChildren(new Command("value") {{
            permission(DefaultPermission.ALL);
            addChildren(value("boolean", new BooleanArgument("v")),
                        value("double", new DoubleArgument("v")),
                        value("duration", new DurationArgument("v")),
                        value("enum", new EnumArgument<>("v", TimeUnit.class)),
                        value("float", new FloatArgument("v")),
                        value("integer", new IntegerArgument("v")),
                        value("long", new LongArgument("v")),
                        value("nameable", new NameableObjectArgument<>("v", List.of(new Fruit("apple")))),
                        value("object", new ObjectArgument<>("v", Map.of("one", 1))),
                        value("string", new StringArgument("v")));
        }});
        return main;
    }

    private static <T> Command value(String name, CommonArgument<T, CommandContext, ?> argument) {
        return new Command(name) {{
            permission(DefaultPermission.ALL);
            argument(argument).execute((ctx, value) -> ctx.sendSuccess("value " + name + "=" + value));
        }};
    }

    private record Fruit(String tabCompleteName) implements Nameable {
    }
}
