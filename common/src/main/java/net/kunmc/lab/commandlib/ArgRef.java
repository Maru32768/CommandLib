package net.kunmc.lab.commandlib;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A handle to a parent argument value, passed to argument child command factories.
 *
 * <pre>{@code
 * argument(new StringArgument("key")).child(key -> new Command("get") {{
 *     execute(ctx -> ctx.sendMessage("get:" + key.get()));
 * }});
 * }</pre>
 *
 * <p>{@link #get()} resolves the value from the command context that is currently being processed on this
 * thread (execute, suggestion, prerequisite and help callbacks). Outside of those callbacks, such as in a task
 * scheduled from an executor, resolve the value beforehand or use {@link #get(CommonCommandContext)}.
 *
 * @param <T> the parsed argument type
 */
public final class ArgRef<T> {
    private final CommonArgument<T, ?, ?> argument;

    private ArgRef(@NotNull CommonArgument<T, ?, ?> argument) {
        this.argument = Objects.requireNonNull(argument);
    }

    @NotNull
    public static <T> ArgRef<T> of(@NotNull CommonArgument<T, ?, ?> argument) {
        return new ArgRef<>(argument);
    }

    @NotNull
    public String name() {
        return argument.name();
    }

    /**
     * Returns the parsed value from the command context currently being processed on this thread.
     *
     * @throws IllegalStateException if called outside of a command callback
     */
    @NotNull
    public T get() {
        CommonCommandContext<?, ?> ctx = CurrentCommandContext.get();
        if (ctx == null) {
            throw new IllegalStateException("ArgRef '" + name() + "' was resolved outside of a command callback. " +
                                                    "Resolve the value inside the callback or use get(ctx).");
        }
        return get(ctx);
    }

    @NotNull
    public T get(@NotNull CommonCommandContext<?, ?> ctx) {
        return Objects.requireNonNull(ctx)
                      .getArgument(argument);
    }

    @Override
    public String toString() {
        return "ArgRef{" + name() + "}";
    }
}
