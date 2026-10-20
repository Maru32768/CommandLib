package dev.marulabs.commandlib.internal.nms.argument;

import com.google.common.collect.Range;
import dev.marulabs.commandlib.internal.nms.NMSClassRegistry;
import dev.marulabs.commandlib.internal.reflection.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class NMSArgumentFloatRange extends NMSArgument<Range<Double>> {
    public static NMSArgumentFloatRange create() {
        return ReflectionUtil.getConstructor(NMSClassRegistry.findClass(NMSArgumentFloatRange.class))
                             .newInstance();
    }

    public NMSArgumentFloatRange(@Nullable Object handle, Class<?> serverClass) {
        super(handle, serverClass);
    }

    /**
     * Converts the bounds of the vanilla {@code float_range}, which are {@link Float} up to 1.16.5 and {@link Double}
     * after, keeping the typed decimal digits: {@code 0.1f} becomes {@code 0.1}, not {@code 0.10000000149011612}.
     */
    protected static Range<Double> doubleRange(@Nullable Number min, @Nullable Number max) {
        return range(min == null ? null : Double.valueOf(min.toString()),
                     max == null ? null : Double.valueOf(max.toString()));
    }
}
