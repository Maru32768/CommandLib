package dev.marulabs.commandlib.util.nms.argument;

import com.google.common.collect.Range;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.util.nms.NMSClass;
import dev.marulabs.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
     * The range argument type of {@code owner}, vanilla's {@code RangeArgument}, whose values are {@code bounds}: the
     * result of its static factory method whose type parses to them. The types are nested classes that Spigot 1.16.5
     * leaves obfuscated and Mohist's plugin remapper cannot map, so the module does not name them.
     *
     * @throws IllegalStateException if {@code owner} has no such method
     */
    protected static ArgumentType<?> rangeArgument(Class<?> owner, Class<?> bounds) {
        for (Method method : owner.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0
                    && parsesTo(method.getReturnType(), bounds)) {
                try {
                    return (ArgumentType<?>) method.invoke(null);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        throw new IllegalStateException(owner + " has no argument type of " + bounds);
    }

    // The type implements RangeArgument<value>.
    private static boolean parsesTo(Class<?> type, Class<?> value) {
        for (Type supertype : type.getGenericInterfaces()) {
            if (supertype instanceof ParameterizedType
                    && ((ParameterizedType) supertype).getActualTypeArguments()[0] == value) {
                return true;
            }
        }
        return false;
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
