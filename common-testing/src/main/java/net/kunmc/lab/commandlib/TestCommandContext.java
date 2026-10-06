package net.kunmc.lab.commandlib;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class TestCommandContext extends CommonCommandContext<FakeSender, String> {
    // The thread-local copy lets CommandTester#executeAndGetContext return its own context when tests run in parallel,
    // while latest() keeps returning contexts created on other threads, e.g. by an asynchronous execute call.
    private static final ThreadLocal<TestCommandContext> LATEST_ON_THREAD = new ThreadLocal<>();
    private static volatile TestCommandContext latest;
    private final List<String> messages = new ArrayList<>();

    TestCommandContext(com.mojang.brigadier.context.CommandContext<FakeSender> ctx) {
        super(ctx);
        LATEST_ON_THREAD.set(this);
        latest = this;
    }

    public static TestCommandContext latest() {
        return latest;
    }

    static TestCommandContext takeLatestOnCurrentThread() {
        TestCommandContext ctx = LATEST_ON_THREAD.get();
        LATEST_ON_THREAD.remove();
        return ctx;
    }

    static void clearLatest() {
        LATEST_ON_THREAD.remove();
        latest = null;
    }

    @Override
    public void sendMessage(@Nullable String message) {
        String value = String.valueOf(message);
        send(value.isEmpty() ? value : "\u00a7f" + value);
    }

    @Override
    public void sendSuccess(@Nullable String message) {
        send("\u00a7a" + String.valueOf(message));
    }

    @Override
    public void sendWarn(@Nullable String message) {
        send("\u00a7e" + String.valueOf(message));
    }

    @Override
    public void sendFailure(@Nullable String message) {
        send("\u00a7c" + String.valueOf(message));
    }

    @Override
    public void sendMessageWithOption(@Nullable String message, @NotNull Consumer<MessageOption> options) {
        send(MessageOption.createMessage(options, (rgb, hoverText) -> legacyColor(rgb) + String.valueOf(message)));
    }

    @Override
    public void sendComponent(String component) {
        send(component);
    }

    @Override
    public @NotNull CommandActor getActor() {
        return handle.getSource();
    }

    @Override
    public @NotNull String getLanguage() {
        String locale = handle.getSource()
                              .getLocale();
        if (locale != null && !locale.isEmpty()) {
            return locale;
        }
        return super.getLanguage();
    }

    public List<String> messages() {
        return List.copyOf(messages);
    }

    private void send(String message) {
        String value = String.valueOf(message);
        messages.add(value);
        handle.getSource()
              .sendMessage(value);
    }

    static String legacyColor(int rgb) {
        String hex = String.format("%06x", rgb & 0xFFFFFF);
        StringBuilder builder = new StringBuilder("\u00a7x");
        for (int i = 0; i < hex.length(); i++) {
            builder.append('\u00a7')
                   .append(hex.charAt(i));
        }
        return builder.toString();
    }
}
