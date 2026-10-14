package dev.marulabs.commandlib.exception;

import dev.marulabs.commandlib.CommonCommandContext;

import java.util.function.Consumer;

public final class CommandPrerequisiteException extends Exception {
    private final Consumer<CommonCommandContext<?, ?>> sendMessage;

    public CommandPrerequisiteException() {
        this("You do not have permission to execute this command.");
    }

    public CommandPrerequisiteException(String message, String... additionalMessages) {
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

    public void sendMessage(CommonCommandContext<?, ?> ctx) {
        sendMessage.accept(ctx);
    }
}
