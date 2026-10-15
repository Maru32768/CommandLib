package dev.marulabs.commandlib.annotation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;

/**
 * A method parameter that an {@link ArgumentFactory} makes an argument for.
 */
public final class AnnotatedParameter {
    private final String name;
    private final Class<?> type;
    private final Parameter parameter;

    AnnotatedParameter(String name, Class<?> type, Parameter parameter) {
        this.name = name;
        this.type = type;
        this.parameter = parameter;
    }

    /**
     * The argument name: the {@link Name} value or the parameter name.
     */
    @NotNull
    public String name() {
        return name;
    }

    /**
     * The value type of the argument: the parameter type with primitives boxed, or the element type of a
     * {@code List} parameter.
     */
    @NotNull
    public Class<?> type() {
        return type;
    }

    @Nullable
    public <A extends Annotation> A annotation(@NotNull Class<A> annotationType) {
        return parameter.getAnnotation(annotationType);
    }

    @NotNull
    public Parameter parameter() {
        return parameter;
    }
}
