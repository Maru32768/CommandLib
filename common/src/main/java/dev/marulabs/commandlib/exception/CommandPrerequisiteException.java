package dev.marulabs.commandlib.exception;

import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.CommonCommandContext;

import java.util.Map;
import java.util.function.Consumer;

public final class CommandPrerequisiteException extends Exception {
    private final Consumer<CommonCommandContext<?, ?>> sendMessage;

    public CommandPrerequisiteException() {
        this(Caption.PERMISSION_DENIED);
    }

    /**
     * Fails with {@code caption} in the sender's locale, see {@link CommonCommandContext#sendFailure(Caption)}.
     */
    public CommandPrerequisiteException(Caption caption) {
        this(caption, Map.of());
    }

    /**
     * Fails with {@code caption} in the sender's locale with its placeholders replaced, see
     * {@link CommonCommandContext#sendFailure(Caption, Map)}.
     */
    public CommandPrerequisiteException(Caption caption, Map<String, ?> placeholders) {
        this(caption.key() + " " + placeholders, ctx -> ctx.sendFailure(caption, placeholders));
    }

    public CommandPrerequisiteException(String message, String... additionalMessages) {
        super(message);
        this.sendMessage = ctx -> {
            ctx.sendFailure(message);
            for (String additionalMessage : additionalMessages) {
                ctx.sendFailure(additionalMessage);
            }
        };
    }

    public CommandPrerequisiteException(Consumer<CommonCommandContext<?, ?>> sendMessage) {
        this.sendMessage = sendMessage;
    }

    // Gives getMessage() a text for logs and handlers; the sender sees what sendMessage sends.
    private CommandPrerequisiteException(String message, Consumer<CommonCommandContext<?, ?>> sendMessage) {
        super(message);
        this.sendMessage = sendMessage;
    }

    public void sendMessage(CommonCommandContext<?, ?> ctx) {
        sendMessage.accept(ctx);
    }
}
