package dev.marulabs.cadence;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.exception.ArgumentParseException;
import dev.marulabs.cadence.internal.ContextState;
import dev.marulabs.cadence.internal.PlatformAdapter;

final class PlatformAdapterImpl implements PlatformAdapter<FakeSender, String, TestCommandContext> {
    @Override
    public TestCommandContext createCommandContext(com.mojang.brigadier.context.CommandContext<FakeSender> ctx,
                                                   ContextState state) {
        return new TestCommandContext(ctx, state);
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
