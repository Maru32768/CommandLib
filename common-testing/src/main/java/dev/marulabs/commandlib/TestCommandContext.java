package dev.marulabs.commandlib;

import dev.marulabs.commandlib.util.ChatColorUtil;
import dev.marulabs.commandlib.util.text.RichText;
import dev.marulabs.commandlib.util.text.TextRichText;
import dev.marulabs.commandlib.util.text.TranslatableRichText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TestCommandContext extends CommonCommandContext<FakeSender, String> {
    // The thread-local copy lets CommandTester#executeAndGetContext return its own context when tests run in parallel,
    // while latest() keeps returning contexts created on other threads, e.g. by an asynchronous execute call.
    private static final ThreadLocal<TestCommandContext> LATEST_ON_THREAD = new ThreadLocal<>();
    private static final ChatColorUtil.ChatColor[] NAMED_COLORS = {ChatColorUtil.BLACK, ChatColorUtil.DARK_BLUE, ChatColorUtil.DARK_GREEN, ChatColorUtil.DARK_AQUA, ChatColorUtil.DARK_RED, ChatColorUtil.DARK_PURPLE, ChatColorUtil.GOLD, ChatColorUtil.GRAY, ChatColorUtil.DARK_GRAY, ChatColorUtil.BLUE, ChatColorUtil.GREEN, ChatColorUtil.AQUA, ChatColorUtil.RED, ChatColorUtil.LIGHT_PURPLE, ChatColorUtil.YELLOW, ChatColorUtil.WHITE};
    private static volatile @Nullable TestCommandContext latest;
    private final List<String> messages = new ArrayList<>();
    private final List<RichText> richMessages = new ArrayList<>();

    TestCommandContext(com.mojang.brigadier.context.CommandContext<FakeSender> ctx) {
        super(ctx);
        LATEST_ON_THREAD.set(this);
        latest = this;
    }

    @Nullable
    public static TestCommandContext latest() {
        return latest;
    }

    @Nullable
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
        sendMessage(RichText.text(String.valueOf(message)));
    }

    @Override
    public void sendSuccess(@Nullable String message) {
        sendSuccess(RichText.text(String.valueOf(message)));
    }

    @Override
    public void sendWarn(@Nullable String message) {
        sendWarn(RichText.text(String.valueOf(message)));
    }

    @Override
    public void sendFailure(@Nullable String message) {
        sendFailure(RichText.text(String.valueOf(message)));
    }

    /**
     * Records {@code text} in {@link #richMessages()} and sends it to the sender as a legacy {@code §} string, the
     * way Bungee's {@code toLegacyText} renders it.
     */
    @Override
    public void sendMessage(@NotNull RichText text) {
        richMessages.add(text);
        // An empty uncolored message stays empty, as sendMessage("") always did.
        boolean blank = text.plainText()
                            .isEmpty()
                && text.color() == null;
        // White, the default chat color.
        send(blank ? "" : legacyText(text, 0xFFFFFF, new boolean[5]));
    }

    /**
     * Sends {@code component}, a legacy {@code §} string on this test platform, unchanged.
     */
    @Override
    public void sendComponent(String component) {
        richMessages.add(RichText.text(String.valueOf(component)));
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

    /**
     * The messages sent through this context as {@link RichText} values, including {@code String} messages, which
     * are recorded as {@code RichText.text(message)} with the color of the send method.
     */
    public List<RichText> richMessages() {
        return List.copyOf(richMessages);
    }

    private void send(String message) {
        String value = String.valueOf(message);
        messages.add(value);
        handle.getSource()
              .sendMessage(value);
    }

    // shortcut: hover and click are dropped and translatables render as their key, since legacy text has neither;
    // assert richMessages() when they matter.
    private static String legacyText(RichText text, int parentColor, boolean[] parentDecorations) {
        int color = text.color() != null ? text.color() : parentColor;
        Boolean[] own = {text.isBold(), text.isItalic(), text.isUnderlined(), text.isStrikethrough(), text.isObfuscated()};
        boolean[] decorations = new boolean[own.length];
        StringBuilder builder = new StringBuilder(legacyColor(color));
        for (int i = 0; i < own.length; i++) {
            decorations[i] = own[i] != null ? own[i] : parentDecorations[i];
            if (decorations[i]) {
                builder.append('\u00a7')
                       .append("lonmk".charAt(i));
            }
        }
        builder.append(text instanceof TextRichText ? ((TextRichText) text).content()
                : ((TranslatableRichText) text).key());
        text.children()
            .forEach(child -> builder.append(legacyText(child, color, decorations)));
        return builder.toString();
    }

    private static String legacyColor(int rgb) {
        for (ChatColorUtil.ChatColor named : NAMED_COLORS) {
            if (Objects.equals(named.getRGB(), rgb)) {
                return named.toString();
            }
        }
        String hex = String.format("%06x", rgb & 0xFFFFFF);
        StringBuilder builder = new StringBuilder("\u00a7x");
        for (int i = 0; i < hex.length(); i++) {
            builder.append('\u00a7')
                   .append(hex.charAt(i));
        }
        return builder.toString();
    }
}
