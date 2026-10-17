package dev.marulabs.commandlib;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.RootCommandNode;
import dev.marulabs.commandlib.exception.ArgumentParseException;

import java.util.List;
import java.util.Map;

public interface PlatformAdapter<S, T, C extends CommonCommandContext<S, T>> {
    /**
     * The platform's adapter. Its class is looked up once.
     */
    @SuppressWarnings("unchecked")
    static <S, T, C extends CommonCommandContext<S, T>> PlatformAdapter<S, T, C> get() {
        return (PlatformAdapter<S, T, C>) PlatformAdapterHolder.get();
    }

    C createCommandContext(com.mojang.brigadier.context.CommandContext<S> ctx);

    /**
     * A context of the source alone, without input or arguments, for a predicate that decides what the source sees.
     */
    default C createSourceContext(S source) {
        return createCommandContext(new com.mojang.brigadier.context.CommandContext<>(source,
                                                                                      "",
                                                                                      Map.of(),
                                                                                      null,
                                                                                      new RootCommandNode<>(),
                                                                                      List.of(),
                                                                                      StringRange.at(0),
                                                                                      null,
                                                                                      null,
                                                                                      false));
    }

    boolean hasPermission(S commandSource, String permissionNode);

    default boolean hasPermission(C ctx, String permissionNode) {
        return hasPermission(ctx.getHandle()
                                .getSource(),
                             permissionNode);
    }

    ArgumentParseException convertCommandSyntaxException(CommandSyntaxException e);
}
