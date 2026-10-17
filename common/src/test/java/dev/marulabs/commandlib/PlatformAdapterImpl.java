package dev.marulabs.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.exception.ArgumentParseException;

public final class PlatformAdapterImpl implements PlatformAdapter<TestCommandSource, String, TestCommandContext> {
    @Override
    public TestCommandContext createCommandContext(com.mojang.brigadier.context.CommandContext<TestCommandSource> ctx) {
        return new TestCommandContext(ctx);
    }

    @Override
    public boolean hasPermission(TestCommandSource commandSource, String permissionNode) {
        if (commandSource != null) {
            return commandSource.hasPermission(permissionNode);
        }
        return true;
    }

    @Override
    public ArgumentParseException convertCommandSyntaxException(CommandSyntaxException e) {
        return new ArgumentParseException(ctx -> ctx.sendFailure(e.getRawMessage()
                                                                  .getString()));
    }
}
