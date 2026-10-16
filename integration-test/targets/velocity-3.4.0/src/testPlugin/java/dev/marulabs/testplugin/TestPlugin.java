package dev.marulabs.testplugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandLib;
import dev.marulabs.commandlib.DefaultPermission;
import dev.marulabs.commandlib.argument.ServerArgument;
import dev.marulabs.commandlib.argument.StringArgument;

/**
 * The commands VelocityIntegrationTest runs from the bot, which joins through the proxy. Velocity has no operators,
 * and no permission plugin is installed, so only the nodes with {@link DefaultPermission#ALL} are the player's.
 */
@Plugin(id = "commandlibtest", name = "CommandLibTest", version = "1.0.0")
public final class TestPlugin {
    private final ProxyServer proxy;

    @Inject
    public TestPlugin(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        CommandLib.register(this, proxy, command());
    }

    private static Command command() {
        Command main = new Command("commandlibtest") {{
            permission(DefaultPermission.ALL);
        }};
        main.addChildren(new Command("echo") {{
            permission(DefaultPermission.ALL);
            argument(new StringArgument("text")).execute((text, ctx) -> ctx.sendSuccess("echo " + text));
        }});
        main.addChildren(new Command("server") {{
            permission(DefaultPermission.ALL);
            argument(new ServerArgument("target")).execute((target,
                                                            ctx) -> ctx.sendSuccess("server "
                                                                    + target.getServerInfo()
                                                                            .getName()));
        }});
        main.addChildren(new Command("consoleOnly") {{
            permission(DefaultPermission.ALL);
            requireConsole();
            execute(ctx -> ctx.sendSuccess("consoleOnly executed"));
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
        return main;
    }
}
