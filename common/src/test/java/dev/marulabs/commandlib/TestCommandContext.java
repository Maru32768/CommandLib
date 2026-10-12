package dev.marulabs.commandlib;

import dev.marulabs.commandlib.util.text.RichText;

import java.util.ArrayList;
import java.util.List;

final class TestCommandContext extends CommonCommandContext<TestCommandSource, String> {
    private static TestCommandContext latest;
    private final List<String> messages = new ArrayList<>();

    TestCommandContext(com.mojang.brigadier.context.CommandContext<TestCommandSource> ctx) {
        super(ctx);
        latest = this;
    }

    static TestCommandContext latest() {
        return latest;
    }

    static void clearLatest() {
        latest = null;
    }

    @Override
    public void sendMessage(String message) {
        messages.add(String.valueOf(message));
    }

    @Override
    public void sendSuccess(String message) {
        messages.add(String.valueOf(message));
    }

    @Override
    public void sendWarn(String message) {
        messages.add(String.valueOf(message));
    }

    @Override
    public void sendFailure(String message) {
        messages.add(String.valueOf(message));
    }

    @Override
    public void sendMessage(RichText text) {
        messages.add(text.plainText());
    }

    @Override
    public void sendComponent(String component) {
        messages.add(component);
    }

    @Override
    public CommandActor getActor() {
        return handle.getSource();
    }

    List<String> messages() {
        return List.copyOf(messages);
    }
}
