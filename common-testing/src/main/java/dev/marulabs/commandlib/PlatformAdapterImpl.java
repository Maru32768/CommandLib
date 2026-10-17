package dev.marulabs.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.exception.ArgumentParseException;

public final class PlatformAdapterImpl implements PlatformAdapter<FakeSender, String, TestCommandContext> {
    @Override
    public TestCommandContext createCommandContext(com.mojang.brigadier.context.CommandContext<FakeSender> ctx) {
        return new TestCommandContext(ctx);
    }

    @Override
    public boolean hasPermission(FakeSender commandSource, String permissionNode) {
        return commandSource.hasPermission(permissionNode);
    }

    @Override
    public ArgumentParseException convertCommandSyntaxException(CommandSyntaxException e) {
        return new ArgumentParseException(ctx -> ctx.sendFailure(e.getRawMessage()
                                                                  .getString()));
    }
}
