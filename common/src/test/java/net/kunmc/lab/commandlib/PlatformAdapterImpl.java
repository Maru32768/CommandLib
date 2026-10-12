package net.kunmc.lab.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;

public final class PlatformAdapterImpl implements PlatformAdapter<TestCommandSource, String, TestCommandContext, TestCommand> {
    @Override
    public TestCommandContext createCommandContext(com.mojang.brigadier.context.CommandContext<TestCommandSource> ctx) {
        return new TestCommandContext(ctx);
    }

    @Override
    public boolean hasPermission(TestCommand command, TestCommandSource commandSource, String permissionPrefix) {
        return hasPermission(commandSource, command.permissionName(permissionPrefix));
    }

    @Override
    public boolean hasPermission(TestCommand command, TestCommandContext ctx, String permissionPrefix) {
        return hasPermission(ctx, command.permissionName(permissionPrefix));
    }

    @Override
    public boolean hasPermission(TestCommandSource commandSource, String permissionNode) {
        if (commandSource instanceof TestCommandSource) {
            return ((TestCommandSource) commandSource).hasPermission(permissionNode);
        }
        return true;
    }

    @Override
    public ArgumentParseException convertCommandSyntaxException(CommandSyntaxException e) {
        return new ArgumentParseException(ctx -> ctx.sendFailure(e.getMessage()));
    }
}
