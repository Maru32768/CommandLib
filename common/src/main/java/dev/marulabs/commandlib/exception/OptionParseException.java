package dev.marulabs.commandlib.exception;

import dev.marulabs.commandlib.CommonCommandContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Thrown when the options of a command are given in a way the command does not accept, such as a missing required
 * option or an option given without the option it requires. Brigadier rejects malformed option values and options
 * the command tree does not offer before the command runs. Arguments fail with {@link ArgumentParseException}
 * instead.
 */
public class OptionParseException extends Exception {
    private final List<String> messages;

    public OptionParseException(String message, String... messages) {
        super(message);
        List<String> list = new ArrayList<>();
        list.add(Objects.requireNonNull(message));
        list.addAll(List.of(messages));
        this.messages = List.copyOf(list);
    }

    /**
     * Sends the messages as failures to {@code ctx}.
     */
    public void sendMessage(CommonCommandContext<?, ?> ctx) {
        messages.forEach(ctx::sendFailure);
    }
}
