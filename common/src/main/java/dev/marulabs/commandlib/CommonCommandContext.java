package dev.marulabs.commandlib;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import dev.marulabs.commandlib.util.ChatColorUtil;
import dev.marulabs.commandlib.util.text.RichText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class CommonCommandContext<S, C> {
    // Replaced only while a variadic argument parses one of its values.
    protected com.mojang.brigadier.context.CommandContext<S> handle;
    private final Map<String, String> argumentNameToInputArgMap = new LinkedHashMap<>();
    private final LinkedHashMap<String, Object> parsedArgMap = new LinkedHashMap<>();
    private final Map<CommandOption<?, ?>, Object> optionValues = new LinkedHashMap<>();
    private final Set<CommandOption<?, ?>> presentOptions = new LinkedHashSet<>();
    private int result = 1;
    // Non-null while CommandRunner collects the messages of a failure.
    private @Nullable List<C> failureMessages;
    private @Nullable List<Runnable> failureSends;

    protected CommonCommandContext(@NotNull com.mojang.brigadier.context.CommandContext<S> ctx) {
        this.handle = Objects.requireNonNull(ctx);

        collectInputs(ctx);
    }

    private void collectInputs(com.mojang.brigadier.context.CommandContext<S> ctx) {
        // Brigadier splits /a <n> <p> sub <b> across parent and child contexts. CommandLib keeps a flat name-to-token
        // map so argument-child executors can still read parent arguments.
        @Nullable
        String previousArgumentName = null;
        for (ParsedCommandNode<S> x : ctx.getNodes()) {
            String name = x.getNode()
                           .getName();
            String input = x.getRange()
                            .get(ctx.getInput());
            // Only an argument repeats: a command literal followed by an argument of the same name is not variadic.
            boolean isArgument = x.getNode() instanceof ArgumentCommandNode;
            boolean repeated = isArgument && name.equals(previousArgumentName);
            previousArgumentName = isArgument ? name : null;
            if (name.startsWith("-") || name.startsWith(CommandOption.INTERNAL_NAME_PREFIX)) {
                continue;
            }

            if (repeated) {
                // A variadic argument repeats its node, so its input is all of its tokens.
                argumentNameToInputArgMap.merge(name, input, (a, b) -> a + " " + b);
            } else {
                argumentNameToInputArgMap.put(name, input);
            }
        }

        com.mojang.brigadier.context.CommandContext<S> child = ctx.getChild();
        if (child != null) {
            collectInputs(child);
        }
    }

    @NotNull
    public final com.mojang.brigadier.context.CommandContext<S> getHandle() {
        return handle;
    }

    @NotNull
    public final String getInput(String name) {
        return argumentNameToInputArgMap.getOrDefault(name, "");
    }

    final boolean hasInput(String name) {
        return argumentNameToInputArgMap.containsKey(name);
    }

    /**
     * Returns the input of the argument at the given index as the sender typed it.
     *
     * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index >= size()})
     */
    @NotNull
    public final String getInput(int index) {
        return getInputs().get(index);
    }

    @NotNull
    public final List<String> getInputs() {
        return new ArrayList<>(argumentNameToInputArgMap.values());
    }

    @NotNull
    public final List<Object> getArguments() {
        return List.copyOf(parsedArgMap.values());
    }

    /**
     * Returns the parsed value of the argument at the given index.
     *
     * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index >= size()})
     */
    @NotNull
    public final Object getArgument(int index) {
        return getArguments().get(index);
    }

    @NotNull
    public final Object getArgument(String name) {
        Object arg = parsedArgMap.get(name);
        if (arg == null) {
            throw new IllegalArgumentException("No such argument '" + name + "' exists on this command");
        }
        return arg;
    }

    /**
     * Returns the parsed value of the argument at the given index, cast to the given type.
     *
     * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index >= size()})
     */
    @NotNull
    public final <T> T getArgument(int index, Class<T> clazz) {
        Object parsedArg = getArgument(index);
        return clazz.cast(parsedArg);
    }

    @NotNull
    public final <T> T getArgument(String name, Class<T> clazz) {
        return clazz.cast(getArgument(name));
    }

    @NotNull
    public final <T> T getArgument(@NotNull CommonArgument<T, ?, ?> argument) {
        Objects.requireNonNull(argument);
        return argument.cast(getArgument(argument.name()));
    }

    @NotNull
    public final <T> T getOption(@NotNull CommandOption<T, ?> option) {
        Objects.requireNonNull(option);
        Object value = optionValues.getOrDefault(option, option.defaultValue());
        return option.cast(value);
    }

    public final boolean hasOption(@NotNull CommandOption<?, ?> option) {
        Objects.requireNonNull(option);
        return presentOptions.contains(option);
    }

    public final void sendMessage(@Nullable Object obj) {
        sendMessage(Objects.toString(obj));
    }

    public abstract void sendMessage(@Nullable String message);

    public final void sendSuccess(@Nullable Object obj) {
        sendSuccess(Objects.toString(obj));
    }

    /**
     * Sends {@code message} in green as command feedback, like {@link #sendSuccess(String, boolean)} without
     * broadcasting it.
     */
    public final void sendSuccess(@Nullable String message) {
        sendSuccess(message, false);
    }

    /**
     * Sends {@code message} in green as command feedback, the way vanilla commands report success: the sender gets it
     * unless the {@code sendCommandFeedback} gamerule is off for a player or the source is silent, such as a function.
     * With {@code broadcastToOps}, the other operators also get it in gray italics, and the server log gets it when
     * the {@code logAdminCommands} gamerule is on, as for {@code /gamemode}. A platform or source without vanilla
     * feedback, such as a test sender, gets it as a plain message.
     */
    public abstract void sendSuccess(@Nullable String message, boolean broadcastToOps);

    public final void sendWarn(@Nullable Object obj) {
        sendWarn(Objects.toString(obj));
    }

    public abstract void sendWarn(@Nullable String message);

    public final void sendFailure(@Nullable Object obj) {
        sendFailure(Objects.toString(obj));
    }

    public abstract void sendFailure(@Nullable String message);

    /**
     * Sends {@code text}, converted to the platform's native component.
     */
    public abstract void sendMessage(@NotNull RichText text);

    /**
     * Sends {@code text} in green unless it has its own color, as command feedback like
     * {@link #sendSuccess(String, boolean)} without broadcasting it.
     */
    public final void sendSuccess(@NotNull RichText text) {
        sendSuccess(text, false);
    }

    /**
     * Sends {@code text} in green unless it has its own color, as command feedback like
     * {@link #sendSuccess(String, boolean)}.
     */
    public abstract void sendSuccess(@NotNull RichText text, boolean broadcastToOps);

    /**
     * Sends {@code text} in yellow unless it has its own color.
     */
    public void sendWarn(@NotNull RichText text) {
        sendMessage(withDefaultColor(text, ChatColorUtil.YELLOW));
    }

    /**
     * Sends {@code text} in red unless it has its own color.
     */
    public void sendFailure(@NotNull RichText text) {
        sendMessage(withDefaultColor(text, ChatColorUtil.RED));
    }

    private static RichText withDefaultColor(@NotNull RichText text, @NotNull ChatColorUtil.ChatColor color) {
        return text.color() != null ? text : text.color(Objects.requireNonNull(color.getRGB()));
    }

    public abstract void sendComponent(C component);

    @NotNull
    public abstract CommandActor getActor();

    /**
     * Returns the sender's Minecraft language code.
     * <p>
     * Player senders should return a code such as {@code ja_jp} or {@code en_us}.
     * Non-player senders fall back to the JVM default locale.
     */
    @NotNull
    public String getLanguage() {
        return Locale.getDefault()
                     .toLanguageTag()
                     .toLowerCase(Locale.ROOT)
                     .replace('-', '_');
    }

    /**
     * Returns the sender's locale converted from {@link #getLanguage()}.
     */
    @NotNull
    public Locale getLocale() {
        return Locale.forLanguageTag(getLanguage().replace('_', '-'));
    }

    /**
     * Sets the result of the command, which {@code /execute store result} stores and which a function or command block
     * gets. It is 1 unless set. A command that fails stores 0 as its result and success: one that throws
     * {@link dev.marulabs.commandlib.exception.CommandPrerequisiteException}, whose preprocess returns false, or whose
     * permission, argument or prerequisite check fails. Its failure messages then reach the sender the way a vanilla
     * command's error does, so a silent source or each target of {@code /execute as @a} does not get them.
     */
    public final void setResult(int result) {
        this.result = result;
    }

    /**
     * Returns the result set by {@link #setResult(int)}, 1 unless set.
     */
    public final int getResult() {
        return result;
    }

    /**
     * Runs {@code send}, which sends {@code message}, unless CommandRunner is collecting the messages of a failure; it
     * then keeps both for the failure. Each platform sends every message through this.
     */
    final void deliver(C message, Runnable send) {
        List<C> messages = failureMessages;
        List<Runnable> sends = failureSends;
        if (messages == null || sends == null) {
            send.run();
            return;
        }
        messages.add(message);
        sends.add(send);
    }

    /**
     * Collects the messages {@code sendMessages} sends into a failure instead of sending them.
     */
    final CommandFailure captureFailure(Runnable sendMessages) {
        List<C> messages = new ArrayList<>();
        List<Runnable> sends = new ArrayList<>();
        failureMessages = messages;
        failureSends = sends;
        try {
            sendMessages.run();
        } finally {
            failureMessages = null;
            failureSends = null;
        }
        return new CommandFailure(() -> toFailureMessage(messages), sends);
    }

    /**
     * Joins the messages of a failure, one per line, into the platform's component, which the platform sends in red.
     */
    abstract Message toFailureMessage(List<C> messages);

    final void setParsedArgument(String name, Object parsedArgument) {
        parsedArgMap.put(name, parsedArgument);
    }

    /**
     * Replaces the handle and the input of the argument {@code name}, so that a variadic argument's element parses one
     * value as if it were the only one.
     */
    final void replaceHandle(com.mojang.brigadier.context.CommandContext<S> handle, String name, String input) {
        this.handle = Objects.requireNonNull(handle);
        argumentNameToInputArgMap.put(name, input);
    }

    final void setOptionValue(CommandOption<?, ?> option, Object value) {
        optionValues.put(option, value);
        presentOptions.add(option);
    }
}
