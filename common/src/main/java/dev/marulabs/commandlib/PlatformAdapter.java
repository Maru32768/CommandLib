package dev.marulabs.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.exception.ArgumentParseException;

import java.lang.reflect.InvocationTargetException;

public interface PlatformAdapter<S, T, C extends CommonCommandContext<S, T>, U extends CommonCommand<C, U>> {
    @SuppressWarnings("unchecked")
    static <S, T, C extends CommonCommandContext<S, T>, U extends CommonCommand<C, U>> PlatformAdapter<S, T, C, U> get() {
        try {
            Class<PlatformAdapter<S, T, C, U>> platformAdapterImplClass = (Class<PlatformAdapter<S, T, C, U>>) Class.forName("dev.marulabs.commandlib.PlatformAdapterImpl");
            return platformAdapterImplClass.getConstructor()
                                           .newInstance();
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new IllegalStateException("Could not find PlatformAdapter", e);
        }
    }

    C createCommandContext(com.mojang.brigadier.context.CommandContext<S> ctx);

    boolean hasPermission(U command, S commandSource, String permissionPrefix);

    boolean hasPermission(U command, C ctx, String permissionPrefix);

    boolean hasPermission(S commandSource, String permissionNode);

    default boolean hasPermission(C ctx, String permissionNode) {
        return hasPermission(ctx.getHandle()
                                .getSource(),
                             permissionNode);
    }

    ArgumentParseException convertCommandSyntaxException(CommandSyntaxException e);
}
