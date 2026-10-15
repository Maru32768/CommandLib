package dev.marulabs.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.exception.ArgumentParseException;

public interface PlatformAdapter<S, T, C extends CommonCommandContext<S, T>, U extends CommonCommand<C, U>> {
    /**
     * The platform's adapter. Its class is looked up once.
     */
    @SuppressWarnings("unchecked")
    static <S, T, C extends CommonCommandContext<S, T>, U extends CommonCommand<C, U>> PlatformAdapter<S, T, C, U> get() {
        return (PlatformAdapter<S, T, C, U>) PlatformAdapterHolder.get();
    }

    C createCommandContext(com.mojang.brigadier.context.CommandContext<S> ctx);

    boolean hasPermission(U command, C ctx, String permissionPrefix);

    boolean hasPermission(S commandSource, String permissionNode);

    default boolean hasPermission(C ctx, String permissionNode) {
        return hasPermission(ctx.getHandle()
                                .getSource(),
                             permissionNode);
    }

    ArgumentParseException convertCommandSyntaxException(CommandSyntaxException e);
}
