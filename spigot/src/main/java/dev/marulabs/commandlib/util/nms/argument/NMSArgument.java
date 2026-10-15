package dev.marulabs.commandlib.util.nms.argument;

import com.google.common.collect.Range;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import org.jetbrains.annotations.Nullable;

public abstract class NMSArgument<T> extends NMSClass {
    public NMSArgument(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    public abstract ArgumentType<?> argument();

    public final T parse(CommandContext<?> ctx, String name) throws CommandSyntaxException {
        try {
            return parseImpl(ctx, name);
        } catch (UncheckedCommandSyntaxException e) {
            throw e.unwrap();
        }
    }

    protected abstract T parseImpl(CommandContext<?> ctx, String name);

    /**
     * Converts the bounds of a vanilla range ({@code MinMaxBounds}), where null means unbounded, to a closed range.
     */
    protected static <C extends Comparable<?>> Range<C> range(@Nullable C min, @Nullable C max) {
        if (min == null) {
            return max == null ? Range.all() : Range.atMost(max);
        }
        return max == null ? Range.atLeast(min) : Range.closed(min, max);
    }
}
