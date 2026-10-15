package dev.marulabs.commandlib;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodType;
import java.util.Objects;

/**
 * The typed key of a value of a command execution: one a command provides with
 * {@link CommonCommand#provide(ContextKey, dev.marulabs.commandlib.command.Extractor)}, or one a middleware puts with
 * {@link CommonCommandContext#put(ContextKey, Object)}. Read it with {@link CommonCommandContext#get(ContextKey)}.
 *
 * <pre>{@code
 * static final ContextKey<Account> ACCOUNT = ContextKey.of(Account.class);
 * static final ContextKey<Instant> STARTED = ContextKey.of("started", Instant.class);
 * }</pre>
 * <p>
 * Two keys are equal when they have the same name and type. {@link #of(Class)} is the key of the type itself, which
 * the {@code Class} overloads of the context and the annotation API use, so {@code ctx.get(Account.class)} reads the
 * value of {@code ContextKey.of(Account.class)}.
 *
 * @param <T> the type of the value
 */
public final class ContextKey<T> {
    private final String name;
    private final Class<T> type;

    private ContextKey(String name, Class<T> type) {
        this.name = name;
        this.type = type;
    }

    /**
     * The key of the type itself. A primitive type stands for its wrapper.
     */
    @NotNull
    public static <T> ContextKey<T> of(@NotNull Class<T> type) {
        return new ContextKey<>("", box(Objects.requireNonNull(type)));
    }

    /**
     * A key named {@code name}, for a second value of a type that already has one, such as two timestamps. A
     * primitive type stands for its wrapper.
     *
     * @throws IllegalArgumentException if {@code name} is empty
     */
    @NotNull
    public static <T> ContextKey<T> of(@NotNull String name, @NotNull Class<T> type) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name must not be empty; use ContextKey.of(type) for the type's key");
        }
        return new ContextKey<>(name, box(Objects.requireNonNull(type)));
    }

    /**
     * The name, empty for the key of a type.
     */
    @NotNull
    public String name() {
        return name;
    }

    @NotNull
    public Class<T> type() {
        return type;
    }

    T cast(Object value) {
        return type.cast(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ContextKey)) {
            return false;
        }
        ContextKey<?> other = (ContextKey<?>) o;
        return name.equals(other.name) && type.equals(other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return name.isEmpty() ? type.getName() : name + " (" + type.getName() + ")";
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> box(Class<T> type) {
        return (Class<T>) MethodType.methodType(type)
                                    .wrap()
                                    .returnType();
    }
}
