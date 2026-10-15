package dev.marulabs.commandlib.exception;

import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.CommonCommandContext;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Thrown when the options of a command are given in a way the command does not accept, such as a missing required
 * option or an option given without the option it requires. Brigadier rejects malformed option values and options
 * the command tree does not offer before the command runs. Arguments fail with {@link ArgumentParseException}
 * instead.
 */
public class OptionParseException extends Exception {
    private final Consumer<CommonCommandContext<?, ?>> sendMessages;

    public OptionParseException(String message, String... messages) {
        super(message);
        List<String> rest = List.of(messages);
        Objects.requireNonNull(message);
        this.sendMessages = ctx -> {
            ctx.sendFailure(message);
            rest.forEach(ctx::sendFailure);
        };
    }

    /**
     * Fails with {@code caption} in the sender's locale with its placeholders replaced, see
     * {@link CommonCommandContext#sendFailure(Caption, Map)}.
     */
    public OptionParseException(Caption caption, Map<String, ?> placeholders) {
        super(caption.key() + " " + placeholders);
        this.sendMessages = ctx -> ctx.sendFailure(caption, placeholders);
    }

    /**
     * Sends the messages as failures to {@code ctx}.
     */
    public void sendMessage(CommonCommandContext<?, ?> ctx) {
        sendMessages.accept(ctx);
    }
}
