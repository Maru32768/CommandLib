package dev.marulabs.commandlib;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tracks the command context being processed on the current thread so that {@link ArgRef#get()} can resolve
 * values without an explicit context. Uses a stack because a command callback may dispatch another command.
 */
final class CurrentCommandContext {
    private static final ThreadLocal<Deque<CommonCommandContext<?, ?>>> STACK = new ThreadLocal<>();

    private CurrentCommandContext() {
    }

    /**
     * Runs {@code action} with {@code ctx} as the current context and returns its result.
     */
    static <R, E extends Exception> R call(@NotNull CommonCommandContext<?, ?> ctx,
                                           @NotNull Action<R, E> action) throws E {
        push(ctx);
        try {
            return action.get();
        } finally {
            pop();
        }
    }

    /**
     * Runs {@code action} with {@code ctx} as the current context.
     */
    static void run(@NotNull CommonCommandContext<?, ?> ctx, @NotNull Runnable action) {
        push(ctx);
        try {
            action.run();
        } finally {
            pop();
        }
    }

    @FunctionalInterface
    interface Action<R, E extends Exception> {
        R get() throws E;
    }

    @Nullable
    static CommonCommandContext<?, ?> get() {
        Deque<CommonCommandContext<?, ?>> stack = STACK.get();
        return stack == null ? null : stack.peek();
    }

    private static void push(CommonCommandContext<?, ?> ctx) {
        Deque<CommonCommandContext<?, ?>> stack = STACK.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            STACK.set(stack);
        }
        stack.push(ctx);
    }

    private static void pop() {
        Deque<CommonCommandContext<?, ?>> stack = STACK.get();
        stack.pop();
        if (stack.isEmpty()) {
            STACK.remove();
        }
    }
}
