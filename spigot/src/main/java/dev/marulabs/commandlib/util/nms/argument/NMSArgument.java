package dev.marulabs.commandlib.util.nms.argument;

import com.google.common.collect.Range;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

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

    /**
     * A bound of a vanilla range from 1.20.2, read through the public {@code min()} or {@code max()} of the
     * {@code MinMaxBounds} interface, whose package 26.2 renamed, so a module for both releases cannot name it.
     *
     * @return the bound, or null when the range is unbounded on that side
     */
    @Nullable
    protected static Object bound(Object bounds, String method) {
        try {
            return ((Optional<?>) bounds.getClass()
                                        .getMethod(method)
                                        .invoke(bounds)).orElse(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
