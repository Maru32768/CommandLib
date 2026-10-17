package dev.marulabs.commandlib;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import dev.marulabs.commandlib.util.text.RichText;

import java.util.ArrayList;
import java.util.List;

final class TestCommandContext extends CommonCommandContext<TestCommandSource, String> {
    private static TestCommandContext latest;
    private final List<String> messages = new ArrayList<>();

    TestCommandContext(com.mojang.brigadier.context.CommandContext<TestCommandSource> ctx) {
        super(ctx);
        // A source context, which the help creates for the visibility predicates, has no input and is no execution.
        if (!ctx.getInput()
                .isEmpty()) {
            latest = this;
        }
    }

    static TestCommandContext latest() {
        return latest;
    }

    static void clearLatest() {
        latest = null;
    }

    @Override
    public void sendMessage(String message) {
        add(String.valueOf(message));
    }

    @Override
    public void sendSuccess(String message, boolean broadcastToOps) {
        add(String.valueOf(message));
    }

    @Override
    public void sendWarn(String message) {
        add(String.valueOf(message));
    }

    @Override
    public void sendFailure(String message) {
        add(String.valueOf(message));
    }

    @Override
    public void sendMessage(RichText text) {
        add(text.plainText());
    }

    @Override
    public void sendSuccess(RichText text, boolean broadcastToOps) {
        add(text.plainText());
    }

    @Override
    public void sendComponent(String component) {
        add(component);
    }

    @Override
    Message toFailureMessage(List<String> messages) {
        return new LiteralMessage(String.join("\n", messages));
    }

    private void add(String message) {
        deliver(message, () -> messages.add(message));
    }

    @Override
    public CommandActor getActor() {
        return handle.getSource();
    }

    List<String> messages() {
        return List.copyOf(messages);
    }
}
