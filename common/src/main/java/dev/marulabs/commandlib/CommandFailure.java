package dev.marulabs.commandlib;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Thrown when a command fails, so that the platform reports the failure messages the way it reports a vanilla
 * command's error, and {@code /execute store success} stores 0. The testers replay the messages instead.
 */
final class CommandFailure extends CommandSyntaxException {
    private static final SimpleCommandExceptionType TYPE = new SimpleCommandExceptionType(new LiteralMessage("Command failed"));
    private final transient Supplier<Message> message;
    private final transient List<Runnable> sends;
    private transient @Nullable Message resolved;
    private transient @Nullable Throwable unexpected;

    /**
     * @param message the platform's message joining the failure messages, created only when the platform reads it,
     *                since testers have no server to create it
     * @param sends   sends each failure message the way the context sends it outside a failure
     */
    CommandFailure(Supplier<Message> message, List<Runnable> sends) {
        super(TYPE, new LiteralMessage(""));
        this.message = message;
        this.sends = List.copyOf(sends);
    }

    @Override
    public Message getRawMessage() {
        if (resolved == null) {
            resolved = message.get();
        }
        return resolved;
    }

    @Override
    public String getMessage() {
        return getRawMessage().getString();
    }

    /**
     * Does not create the platform's message: a middleware stage wraps the failure in a CompletionException, whose
     * message is this text, and the testers have no server to create the message with.
     */
    @Override
    public String toString() {
        return getClass().getName();
    }

    boolean hasMessages() {
        return !sends.isEmpty();
    }

    /**
     * Sends the failure messages to the sender, as testers do in place of the platform.
     */
    void sendMessages() {
        sends.forEach(Runnable::run);
    }

    /**
     * Records the exception that a middleware, the executor or the help threw unexpectedly.
     */
    CommandFailure unexpected(Throwable e) {
        this.unexpected = e;
        return this;
    }

    /**
     * Throws the {@link AssertionError} that a middleware, the executor or the help threw, as testers do so that a
     * failed assertion in a command fails the test.
     */
    void rethrowAssertionError() {
        if (unexpected instanceof AssertionError) {
            throw (AssertionError) unexpected;
        }
    }
}
