package dev.marulabs.commandlib;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A failure that CommandLib reports, for an {@link ErrorFormatter}.
 */
public final class CommandError {
    /**
     * What failed.
     */
    public enum Kind {
        /** The sender lacks the permission of the command or of an argument branch on the path. */
        PERMISSION_DENIED,
        /** A required option is missing or an option dependency is not met ({@code OptionParseException}). */
        OPTION,
        /** An argument's value was rejected while converting it ({@code ArgumentParseException}). */
        ARGUMENT,
        /** A validator rejected an argument's value ({@code ArgumentValidationException}). */
        VALIDATION,
        /**
         * A prerequisite, an extractor, a middleware, the executor or the default value of an omitted optional
         * argument threw {@code CommandPrerequisiteException}.
         */
        PREREQUISITE,
        /** The executor's stage failed with a {@code TimeoutException}. */
        TIMEOUT,
        /** Any other exception of a middleware, the executor or the help. */
        UNEXPECTED
    }

    private final Kind kind;
    private final @Nullable Throwable exception;
    private final CommandHelp.@Nullable Usage usage;
    private final Runnable sendDefault;

    CommandError(Kind kind, @Nullable Throwable exception, CommandHelp.@Nullable Usage usage, Runnable sendDefault) {
        this.kind = kind;
        this.exception = exception;
        this.usage = usage;
        this.sendDefault = sendDefault;
    }

    @NotNull
    public Kind kind() {
        return kind;
    }

    /**
     * The exception that carries the failure, or null for {@link Kind#PERMISSION_DENIED}.
     */
    @Nullable
    public Throwable exception() {
        return exception;
    }

    /**
     * For an {@link Kind#ARGUMENT} or {@link Kind#VALIDATION} error, the usage of the argument branch whose argument
     * failed, which the sender has the permission for; null for the other kinds.
     */
    public CommandHelp.@Nullable Usage usage() {
        return usage;
    }

    /**
     * Sends the messages CommandLib sends for this failure by default, such as the exception's message or
     * {@link Caption#PERMISSION_DENIED}.
     */
    public void sendDefault() {
        sendDefault.run();
    }
}
