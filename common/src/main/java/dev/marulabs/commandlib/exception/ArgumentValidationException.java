package dev.marulabs.commandlib.exception;

import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.CommonCommandContext;

import java.util.Map;
import java.util.function.Consumer;

public class ArgumentValidationException extends ArgumentParseException {
    public static <C extends CommonCommandContext<?, ?>> ArgumentValidationException ofIncorrectInput(String argumentName,
                                                                                                      C ctx,
                                                                                                      String incorrectInput) {
        return new ArgumentValidationException(buildIncorrectInputMessage(argumentName, ctx, incorrectInput));
    }

    public ArgumentValidationException() {
        this(Caption.INVALID_VALUE, Map.of());
    }

    /**
     * Rejects the value with {@code caption} in the sender's locale with its placeholders replaced, see
     * {@link CommonCommandContext#sendFailure(Caption, Map)}.
     */
    public ArgumentValidationException(Caption caption, Map<String, ?> placeholders) {
        super(caption, placeholders);
    }

    public ArgumentValidationException(String message, String... messages) {
        super(message, messages);
    }

    public ArgumentValidationException(Consumer<CommonCommandContext<?, ?>> sendMessages) {
        super(sendMessages);
    }
}
