package dev.marulabs.commandlib;

import dev.marulabs.commandlib.util.ChatColorUtil;
import dev.marulabs.commandlib.util.text.RichText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class CommonCommandContext<S, C> {
    protected final com.mojang.brigadier.context.CommandContext<S> handle;
    private final Map<String, String> argumentNameToInputArgMap = new LinkedHashMap<>();
    private final LinkedHashMap<String, Object> parsedArgMap = new LinkedHashMap<>();
    private final Map<CommandOption<?, ?>, Object> optionValues = new LinkedHashMap<>();
    private final Set<CommandOption<?, ?>> presentOptions = new LinkedHashSet<>();

    protected CommonCommandContext(@NotNull com.mojang.brigadier.context.CommandContext<S> ctx) {
        this.handle = Objects.requireNonNull(ctx);

        collectInputs(ctx);
    }

    private void collectInputs(com.mojang.brigadier.context.CommandContext<S> ctx) {
        // Brigadier splits /a <n> <p> sub <b> across parent and child contexts. CommandLib keeps a flat name-to-token
        // map so argument-child executors can still read parent arguments.
        ctx.getNodes()
           .forEach(x -> {
               String name = x.getNode()
                              .getName();
               if (name.startsWith("-") || name.startsWith(CommandOption.INTERNAL_NAME_PREFIX)) {
                   return;
               }

               argumentNameToInputArgMap.put(name,
                                             x.getRange()
                                              .get(ctx.getInput()));
           });

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

    public abstract void sendSuccess(@Nullable String message);

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
     * Sends {@code text} in green unless it has its own color.
     */
    public void sendSuccess(@NotNull RichText text) {
        sendMessage(withDefaultColor(text, ChatColorUtil.GREEN));
    }

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

    final void setParsedArgument(String name, Object parsedArgument) {
        parsedArgMap.put(name, parsedArgument);
    }

    final void setOptionValue(CommandOption<?, ?> option, Object value) {
        optionValues.put(option, value);
        presentOptions.add(option);
    }
}
