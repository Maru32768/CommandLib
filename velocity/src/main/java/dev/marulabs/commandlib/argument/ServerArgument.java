package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.StringUtil;

/**
 * A server registered with the proxy, by name, ignoring case. Suggests the names of the registered servers. The input
 * is a word or a {@code "quoted name"}, so that any server name can be given.
 */
public class ServerArgument extends Argument<RegisteredServer, ServerArgument> {
    public ServerArgument(String name) {
        super(name, StringArgumentType.string());

        addSuggestionProvider(sb -> {
            sb.getContext()
              .getProxy()
              .getAllServers()
              .stream()
              .filter(filter(sb.getContext()))
              .map(x -> x.getServerInfo()
                         .getName())
              .filter(x -> sb.getLatestInput()
                             .isEmpty()
                      || StringUtil.containsIgnoreCase(x, sb.getLatestInput()))
              .forEach(sb::suggest);
        });
    }

    @Override
    public RegisteredServer cast(Object parsedArgument) {
        return (RegisteredServer) parsedArgument;
    }

    @Override
    protected RegisteredServer parseImpl(CommandContext ctx) throws ArgumentParseException {
        String s = StringArgumentType.getString(ctx.getHandle(), name());
        return ctx.getProxy()
                  .getServer(s)
                  .orElseThrow(() -> ArgumentParseException.ofIncorrectInput(name(), ctx, s));
    }
}
