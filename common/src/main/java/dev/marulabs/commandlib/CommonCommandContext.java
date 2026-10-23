package dev.marulabs.commandlib;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import dev.marulabs.commandlib.util.ChatColorUtil;
import dev.marulabs.commandlib.util.text.RichText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * The context of one command execution.
 * <p>
 * Thread safety: the parsed arguments, options and inputs do not change once the executor runs, so any thread may read
 * them. Everything else, sending messages, the result, the sender and the platform objects, belongs to the sync
 * thread; from async work, return to it with {@link #supplyAsync(Supplier)} or {@link #syncExecutor()}.
 */
public abstract class CommonCommandContext<S, C> {
    // Replaced only while a variadic argument parses one of its values.
    protected com.mojang.brigadier.context.CommandContext<S> handle;
    // The inputs of the command literals and the arguments, in input order; a later node replaces an earlier one of the
    // same name.
    private final Map<String, String> argumentNameToInputArgMap = new LinkedHashMap<>();
    // The inputs of the argument nodes only. A command literal can share an argument's name, so the map above can hold
    // the literal's text under the argument's name.
    private final Map<String, String> argumentInputs = new HashMap<>();
    private final LinkedHashMap<String, Object> parsedArgMap = new LinkedHashMap<>();
    private final Map<CommandOption<?, ?>, Object> optionValues = new LinkedHashMap<>();
    private final Set<CommandOption<?, ?>> presentOptions = new LinkedHashSet<>();
    private int result = 1;
    // Non-null while CommandRunner collects the messages of a failure.
    private @Nullable List<C> failureMessages;
    private @Nullable List<Runnable> failureSends;
    // Set by CommandRunner before the command runs; null in suggestion contexts.
    private @Nullable CommandScheduler scheduler;
    // The parse this execution came from, which the targets of one forked execution share.
    private @Nullable Object parse;
    // Set by CommandRunner and the help to those of the command; the default in suggestion contexts.
    private Captions captions = Captions.DEFAULT;
    // The values put by middlewares and those the providers gave so far.
    private final Map<ContextKey<?>, Object> values = new HashMap<>();
    // Set by the node creators from the command, before any user code runs.
    private Map<ContextKey<?>, ? extends Extractor<?, ?>> providers = Map.of();
    // The keys whose provider is running, to report a provider that needs its own value.
    private final Set<ContextKey<?>> resolving = new HashSet<>();

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
            // Up to 1.20.2 vanilla's text component argument ends its range one past the end of the input.
            String input = ctx.getInput()
                              .substring(x.getRange()
                                          .getStart(),
                                         Math.min(x.getRange()
                                                   .getEnd(),
                                                  ctx.getInput()
                                                     .length()));
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
                argumentInputs.merge(name, input, (a, b) -> a + " " + b);
            } else {
                argumentNameToInputArgMap.put(name, input);
                if (isArgument) {
                    argumentInputs.put(name, input);
                }
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

    /**
     * Returns the input of the argument {@code name} as the sender typed it, or of the command literal {@code name} when
     * no such argument was given, or {@code ""} when neither was.
     */
    @NotNull
    public final String getInput(String name) {
        String input = argumentInputs.get(name);
        return input != null ? input : argumentNameToInputArgMap.getOrDefault(name, "");
    }

    /**
     * Whether the input has a value for the argument {@code name}, not only a command literal of that name.
     */
    final boolean isArgumentGiven(String name) {
        return argumentInputs.containsKey(name);
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

    /**
     * Returns the value of {@code option}: the given value, or its default when it is not given.
     * <p>
     * Suggestion providers and {@code visibleWhen} predicates run without the options being parsed, so there it gives
     * the default, also for an option typed before the argument being completed.
     *
     * @throws IllegalArgumentException if no command registers the option, such as a copy
     *                                  {@link CommandOption#repeatable(int)} returns of a registered option
     * @throws IllegalStateException    if the option was not given and has no default here
     */
    @NotNull
    public final <T> T getOption(@NotNull CommandOption<T, ?> option) {
        Objects.requireNonNull(option);
        if (!option.registered) {
            throw new IllegalArgumentException(option.formatName()
                    + " is not registered with any command; read the option that option(...) returned");
        }
        Object value = optionValues.containsKey(option) ? optionValues.get(option) : option.defaultValue();
        if (value == null) {
            // An option of an argument has no default outside an execution, or none at all when it is required.
            throw new IllegalStateException(option.formatName() + " was not given and has no default value here.");
        }
        return option.cast(value);
    }

    /**
     * Whether {@code option} was given. Like {@link #getOption(CommandOption)}, it does not see the options in
     * suggestion providers and {@code visibleWhen} predicates, where it is {@code false}.
     */
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
     * Sends {@code message} in green as command feedback, the way vanilla commands report success: the sender gets it
     * unless the {@code sendCommandFeedback} gamerule is off for a player or the source is silent, such as a function.
     * A platform or source without vanilla feedback, such as a test sender, gets it as a plain message.
     */
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
     * Sends {@code text} in green unless it has its own color, as command feedback like {@link #sendSuccess(String)}.
     */
    public abstract void sendSuccess(@NotNull RichText text);

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

    /**
     * Returns the text of {@code caption} for the sender's {@link #getLocale() locale}, from the {@link Captions} of
     * the command.
     */
    @NotNull
    public final String caption(@NotNull Caption caption) {
        return caption(caption, Map.of());
    }

    /**
     * Returns the text of {@code caption} for the sender's {@link #getLocale() locale}, from the {@link Captions} of
     * the command, with each {@code {name}} replaced by the value of {@code name} in {@code placeholders}.
     */
    @NotNull
    public final String caption(@NotNull Caption caption, @NotNull Map<String, ?> placeholders) {
        return captions.format(getLocale(), caption, placeholders);
    }

    /**
     * Sends {@code caption} as {@link #sendMessage(RichText)} does, through the {@link CaptionSender} of the
     * command's {@link Captions}.
     */
    public final void sendMessage(@NotNull Caption caption) {
        sendMessage(caption, Map.of());
    }

    /**
     * Sends {@code caption} with its placeholders replaced as {@link #sendMessage(RichText)} does, through the
     * {@link CaptionSender} of the command's {@link Captions}.
     */
    public final void sendMessage(@NotNull Caption caption, @NotNull Map<String, ?> placeholders) {
        sendCaption(caption, placeholders, null, this::sendMessage);
    }

    /**
     * Sends {@code caption} as {@link #sendSuccess(RichText)} does, through the {@link CaptionSender} of the
     * command's {@link Captions}.
     */
    public final void sendSuccess(@NotNull Caption caption) {
        sendSuccess(caption, Map.of());
    }

    /**
     * Sends {@code caption} with its placeholders replaced as {@link #sendSuccess(RichText)} does, through the
     * {@link CaptionSender} of the command's {@link Captions}.
     */
    public final void sendSuccess(@NotNull Caption caption, @NotNull Map<String, ?> placeholders) {
        sendCaption(caption, placeholders, null, this::sendSuccess);
    }

    /**
     * Sends {@code caption} as {@link #sendWarn(RichText)} does, through the {@link CaptionSender} of the command's
     * {@link Captions}.
     */
    public final void sendWarn(@NotNull Caption caption) {
        sendWarn(caption, Map.of());
    }

    /**
     * Sends {@code caption} with its placeholders replaced as {@link #sendWarn(RichText)} does, through the
     * {@link CaptionSender} of the command's {@link Captions}.
     */
    public final void sendWarn(@NotNull Caption caption, @NotNull Map<String, ?> placeholders) {
        sendCaption(caption, placeholders, null, this::sendWarn);
    }

    /**
     * Sends {@code caption} as {@link #sendFailure(RichText)} does, through the {@link CaptionSender} of the
     * command's {@link Captions}.
     */
    public final void sendFailure(@NotNull Caption caption) {
        sendFailure(caption, Map.of());
    }

    /**
     * Sends {@code caption} with its placeholders replaced as {@link #sendFailure(RichText)} does, through the
     * {@link CaptionSender} of the command's {@link Captions}.
     */
    public final void sendFailure(@NotNull Caption caption, @NotNull Map<String, ?> placeholders) {
        sendCaption(caption, placeholders, null, this::sendFailure);
    }

    final void sendCaption(Caption caption,
                           Map<String, ?> placeholders,
                           @Nullable RichText suffix,
                           java.util.function.Consumer<RichText> send) {
        captions.send(this, caption, placeholders, suffix, send);
    }

    /**
     * Fails the command with {@code message} and each of {@code additionalMessages} in red, as throwing
     * {@link CommandPrerequisiteException} does, also from a typed executor ({@code (target, ctx) -> ...}) or another
     * lambda that cannot throw it. It never returns; write {@code throw ctx.fail(...)} to end a branch.
     */
    @NotNull
    public final RuntimeException fail(@NotNull String message, @NotNull String... additionalMessages) {
        throw fail(new CommandPrerequisiteException(message, additionalMessages));
    }

    /**
     * Same as {@link #fail(String, String...)} with {@code caption} in the sender's locale.
     */
    @NotNull
    public final RuntimeException fail(@NotNull Caption caption) {
        throw fail(new CommandPrerequisiteException(caption));
    }

    /**
     * Same as {@link #fail(String, String...)} with {@code caption} in the sender's locale and its placeholders
     * replaced.
     */
    @NotNull
    public final RuntimeException fail(@NotNull Caption caption, @NotNull Map<String, ?> placeholders) {
        throw fail(new CommandPrerequisiteException(caption, placeholders));
    }

    /**
     * Throws {@code exception} although the method does not declare it, so that the command fails with its message as
     * if the caller had thrown it.
     */
    @NotNull
    public final RuntimeException fail(@NotNull CommandPrerequisiteException exception) {
        throw CommonCommandContext.<RuntimeException>sneakyThrow(Objects.requireNonNull(exception));
    }

    final Captions captions() {
        return captions;
    }

    final void captions(Captions captions) {
        this.captions = Objects.requireNonNull(captions);
    }

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
     * <p>
     * An {@code executeAsync} command whose stage has not completed when the dispatch returns gives the result set by
     * then, so setting it later has no effect.
     */
    public final void setResult(int result) {
        this.result = result;
    }

    /**
     * Runs {@code work} on the {@link #asyncExecutor()} and returns a future that completes on the
     * {@link #syncExecutor()}, so that the stages chained on it with the non-async methods, such as
     * {@code thenAccept}, run on the sync thread and may send messages and use the platform API.
     *
     * @throws IllegalStateException outside a command execution, such as in a suggestion provider
     */
    @NotNull
    public final <T> CompletableFuture<T> supplyAsync(@NotNull Supplier<T> work) {
        return CompletableFuture.supplyAsync(Objects.requireNonNull(work), asyncExecutor())
                                .whenCompleteAsync((x, e) -> {
                                }, syncExecutor());
    }

    /**
     * Same as {@link #supplyAsync(Supplier)} for work without a value.
     */
    @NotNull
    public final CompletableFuture<Void> runAsync(@NotNull Runnable work) {
        Objects.requireNonNull(work);
        return supplyAsync(() -> {
            work.run();
            return null;
        });
    }

    /**
     * The executor for the thread that owns the sender, the server thread. It runs a task at once when called on that
     * thread.
     *
     * @throws IllegalStateException outside a command execution, such as in a suggestion provider
     */
    @NotNull
    public final Executor syncExecutor() {
        return requireScheduler().sync(this);
    }

    /**
     * The executor for work off the sync thread.
     *
     * @throws IllegalStateException outside a command execution, such as in a suggestion provider
     */
    @NotNull
    public final Executor asyncExecutor() {
        return requireScheduler().async(this);
    }

    private CommandScheduler requireScheduler() {
        CommandScheduler scheduler = this.scheduler;
        if (scheduler == null) {
            throw new IllegalStateException("The executors are available only while a command executes.");
        }
        return scheduler;
    }

    final void scheduler(CommandScheduler scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler);
    }

    final void parse(Object parse) {
        this.parse = Objects.requireNonNull(parse);
    }

    @Nullable
    final Object parse() {
        return parse;
    }

    /**
     * Sets the value of {@code key} for the rest of this execution, replacing the value the command's provider gives,
     * so that later middlewares, the executor and the parameters of annotated methods read it with
     * {@link #get(ContextKey)}. Like the sender, the values belong to the sync thread.
     */
    public final <T> void put(@NotNull ContextKey<T> key, @NotNull T value) {
        values.put(Objects.requireNonNull(key), key.cast(Objects.requireNonNull(value)));
    }

    /**
     * Same as {@link #put(ContextKey, Object)} with the key of {@code type}.
     */
    public final <T> void put(@NotNull Class<T> type, @NotNull T value) {
        put(ContextKey.of(type), value);
    }

    /**
     * Returns the value of {@code key}: the one a middleware put, or else the one the command's provider gives, which
     * runs once per execution (see {@link CommonCommand#provide(ContextKey, Extractor)}).
     * <p>
     * A {@link CommandPrerequisiteException} thrown by the provider, such as for a sender who is not a player, is
     * thrown from this method as it is, although the method does not declare it, so that the command fails with its
     * message also when it is called from a lambda that cannot throw it.
     *
     * @throws IllegalStateException if nothing provides or put the value
     */
    @NotNull
    public final <T> T get(@NotNull ContextKey<T> key) {
        return find(key).orElseThrow(() -> new IllegalStateException("No value for " + key
                + " in this command execution; provide one with provide(...) on the command or put(...) in a "
                + "middleware"));
    }

    /**
     * Same as {@link #get(ContextKey)} with the key of {@code type}, such as {@code ctx.get(GamePlayer.class)}.
     */
    @NotNull
    public final <T> T get(@NotNull Class<T> type) {
        return get(ContextKey.of(type));
    }

    /**
     * Same as {@link #get(ContextKey)}, but empty when nothing provides or put the value.
     */
    @NotNull
    public final <T> Optional<T> find(@NotNull ContextKey<T> key) {
        Objects.requireNonNull(key);
        Object value = values.get(key);
        if (value != null) {
            return Optional.of(key.cast(value));
        }
        Extractor<?, ?> provider = providers.get(key);
        if (provider == null) {
            return Optional.empty();
        }
        if (!resolving.add(key)) {
            throw new IllegalStateException("The provider of " + key + " reads its own value");
        }
        Object provided;
        try {
            provided = extract(provider);
        } catch (CommandPrerequisiteException e) {
            throw CommonCommandContext.sneakyThrow(e);
        } finally {
            resolving.remove(key);
        }
        T typed = key.cast(Objects.requireNonNull(provided, () -> "The provider of " + key + " returned null"));
        values.put(key, typed);
        return Optional.of(typed);
    }

    /**
     * Same as {@link #find(ContextKey)} with the key of {@code type}.
     */
    @NotNull
    public final <T> Optional<T> find(@NotNull Class<T> type) {
        return find(ContextKey.of(type));
    }

    // The providers come from a command of this context's type, as the node creators set them.
    @SuppressWarnings("unchecked")
    private Object extract(Extractor<?, ?> provider) throws CommandPrerequisiteException {
        return ((Extractor<CommonCommandContext<S, C>, ?>) provider).extract(this);
    }

    // Rethrows a checked exception without declaring it; the cast is erased, so nothing is checked.
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    final void providers(Map<ContextKey<?>, ? extends Extractor<?, ?>> providers) {
        this.providers = Objects.requireNonNull(providers);
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
        } catch (Throwable e) {
            // The messages sent before the exception still reach the sender, ahead of what the exception reports.
            failureMessages = null;
            failureSends = null;
            sends.forEach(Runnable::run);
            throw e;
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
     * Replaces the handle and the input of the argument {@code name}, also in {@link #getInputs()}, so that a variadic
     * argument's element or an option's value parses one value as if it were the only one.
     *
     * @return puts the previous handle and inputs back
     */
    final Runnable replaceHandle(com.mojang.brigadier.context.CommandContext<S> handle, String name, String input) {
        com.mojang.brigadier.context.CommandContext<S> previousHandle = this.handle;
        this.handle = Objects.requireNonNull(handle);
        @Nullable
        String previousInput = argumentInputs.put(name, input);
        @Nullable
        String previousNodeInput = argumentNameToInputArgMap.put(name, input);
        return () -> {
            this.handle = previousHandle;
            putBack(argumentInputs, name, previousInput);
            putBack(argumentNameToInputArgMap, name, previousNodeInput);
        };
    }

    // Put, not removed and added again, so the input keeps its place in getInputs().
    private static void putBack(Map<String, String> inputs, String name, @Nullable String input) {
        if (input == null) {
            inputs.remove(name);
        } else {
            inputs.put(name, input);
        }
    }

    final void setOptionValue(CommandOption<?, ?> option, Object value) {
        optionValues.put(option, value);
        presentOptions.add(option);
    }

    /**
     * Sets the value of an option that was not given, such as the default of an option of an optional argument.
     */
    final void setOptionDefault(CommandOption<?, ?> option, Object value) {
        optionValues.put(option, value);
    }
}
