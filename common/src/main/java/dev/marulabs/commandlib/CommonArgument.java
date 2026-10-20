package dev.marulabs.commandlib;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import dev.marulabs.commandlib.argument.ArgumentValidator;
import dev.marulabs.commandlib.command.AsyncCommandExecutor;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.exception.ArgumentValidationException;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import dev.marulabs.commandlib.suggestion.SuggestionProvider;
import dev.marulabs.commandlib.util.UncaughtExceptionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class CommonArgument<T, C extends CommonCommandContext<?, ?>, SELF extends CommonArgument<T, C, SELF>> {
    private final String name;
    private final List<ArgumentSuggestionProvider<C>> suggestionProviders = new ArrayList<>();
    // Returns null when it cannot parse the input either.
    private BiFunction<C, String, T> additionalParser = (ctx, input) -> null;
    // Null until execute is called.
    private @Nullable AsyncCommandExecutor<C> executor;
    private final ArgumentType<?> type;
    private ArgumentValidator<? super T, C> validator = (x, ctx) -> {
    };
    private BiFunction<? super T, C, ? extends T> transformer = (x, ctx) -> x;
    private final List<UncaughtExceptionHandler<C>> uncaughtExceptionHandlers = new ArrayList<>();
    // Null unless optional is called; gives the value of the omitted argument.
    private @Nullable Extractor<C, ? extends T> defaultValue;

    protected CommonArgument(@NotNull String name, @NotNull ArgumentType<?> type) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
        this.suggestionProviders.add(ArgumentSuggestionProvider.defaults());
    }

    public final String name() {
        return name;
    }

    final List<ArgumentSuggestionProvider<C>> suggestionProviders() {
        return List.copyOf(suggestionProviders);
    }

    @SuppressWarnings("unchecked")
    public final SELF addSuggestionProvider(@NotNull SuggestionProvider<C> suggestionProvider) {
        this.suggestionProviders.add(ArgumentSuggestionProvider.custom(suggestionProvider));
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public final SELF setSuggestionProvider(@NotNull SuggestionProvider<C> suggestionProvider) {
        Objects.requireNonNull(suggestionProvider,
                               "suggestionProvider must not be null; use clearSuggestionProviders() to remove all suggestions");
        this.suggestionProviders.clear();
        this.suggestionProviders.add(ArgumentSuggestionProvider.custom(suggestionProvider));
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public final SELF clearSuggestionProviders() {
        this.suggestionProviders.clear();
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public final SELF useDefaultSuggestions() {
        boolean alreadyUsesDefaults = this.suggestionProviders.stream()
                                                              .anyMatch(ArgumentSuggestionProvider::isDefaultSuggestions);
        if (!alreadyUsesDefaults) {
            this.suggestionProviders.add(ArgumentSuggestionProvider.defaults());
        }
        return (SELF) this;
    }

    /**
     * Swaps the argument type's default suggestions, where they are enabled, for the given action and keeps every
     * other suggestion provider in place.
     */
    protected final void replaceDefaultSuggestions(@NotNull SuggestionProvider<C> suggestionProvider) {
        Objects.requireNonNull(suggestionProvider);
        this.suggestionProviders.replaceAll(x -> x.isDefaultSuggestions()
                ? ArgumentSuggestionProvider.custom(suggestionProvider) : x);
    }

    final void copySuggestionProviders(@NotNull CommonArgument<?, C, ?> source) {
        this.suggestionProviders.clear();
        this.suggestionProviders.addAll(source.suggestionProviders);
    }

    /**
     * Makes this argument optional: when the sender omits it, the callback receives {@code defaultValue}. The default
     * is used as is, without the validator or the transformer. Only the last arguments of a branch can be optional,
     * and such a branch cannot have child commands; registering the command fails otherwise.
     *
     * <pre>{@code
     * argument(new StringArgument("item"), new IntegerArgument("count").optional(1)).execute((item, count, ctx) -> {
     *     ctx.sendMessage(item + " x" + count);
     * });
     * }</pre>
     */
    public final SELF optional(@NotNull T defaultValue) {
        Objects.requireNonNull(defaultValue);
        return optional(ctx -> defaultValue);
    }

    /**
     * Same as {@link #optional(Object)}, and computes the default from the context each time the argument is omitted,
     * for example {@code optional(Command.playerRequirer())} to target the sender. A
     * {@link CommandPrerequisiteException} thrown by the extractor is sent to the sender and stops the command. The
     * extractor must not return null.
     */
    @SuppressWarnings("unchecked")
    public final SELF optional(@NotNull Extractor<C, ? extends T> defaultValue) {
        this.defaultValue = Objects.requireNonNull(defaultValue);
        return (SELF) this;
    }

    final boolean isOptional() {
        return defaultValue != null;
    }

    final T defaultValue(C ctx) throws CommandPrerequisiteException {
        Extractor<C, ? extends T> defaultValue = Objects.requireNonNull(this.defaultValue);
        return Objects.requireNonNull(defaultValue.extract(ctx),
                                      () -> "The default value of argument '" + name + "' must not be null");
    }

    /**
     * Returns an argument that takes this argument one or more times and gives the values as a list, such as
     * {@code new PlayerArgument("targets").variadic()} for {@code /heal <targets...>}. It accepts up to
     * {@value VariadicArgument#DEFAULT_MAX_COUNT} values; use {@link #variadic(int)} to change the limit. Only the last
     * argument of a branch can be variadic, and such a branch cannot have child commands; registering the command
     * fails otherwise.
     *
     * <p>Configure each value (validator, transformer, suggestions) on this argument before calling {@code variadic},
     * and the list on the returned argument. Make it optional with {@code variadic().optional(List.of())}.
     */
    @NotNull
    public final VariadicArgument<T, C> variadic() {
        return variadic(VariadicArgument.DEFAULT_MAX_COUNT);
    }

    /**
     * Same as {@link #variadic()}, and accepts up to {@code maxCount} values. Brigadier has no repetition, so the
     * command tree holds one node per accepted value.
     *
     * @throws IllegalArgumentException if {@code maxCount} is less than 1
     * @throws IllegalStateException    if this argument is already variadic, or is optional, has an executor or has
     *                                  uncaught exception handlers, which apply to the returned argument instead
     */
    @NotNull
    public final VariadicArgument<T, C> variadic(int maxCount) {
        if (this instanceof VariadicArgument) {
            throw new IllegalStateException("Argument '" + name + "' is already variadic");
        }
        // The branch reads these from the variadic argument, so they would be dropped silently.
        if (isOptional() || executor != null || !uncaughtExceptionHandlers.isEmpty()) {
            throw new IllegalStateException("Argument '" + name
                    + "' is optional, has an executor or has uncaught exception handlers; set them on the argument "
                    + "variadic() returns");
        }
        return new VariadicArgument<>(this, maxCount);
    }

    /**
     * The number of command nodes this argument takes in a branch.
     */
    int nodeCount() {
        return 1;
    }

    @SuppressWarnings("unchecked")
    public final SELF additionalParser(@NotNull BiFunction<C, String, T> parser) {
        this.additionalParser = Objects.requireNonNull(parser);
        return (SELF) this;
    }

    @Nullable
    protected final AsyncCommandExecutor<C> executor() {
        return executor;
    }

    protected final List<UncaughtExceptionHandler<C>> uncaughtExceptionHandlers() {
        return List.copyOf(uncaughtExceptionHandlers);
    }

    public final SELF execute(@NotNull CommandExecutor<C> executor) {
        return executeAsync(AsyncCommandExecutor.of(executor));
    }

    /**
     * Sets an executor whose work finishes later, like {@link CommonCommand#executeAsync(AsyncCommandExecutor)}.
     *
     * @throws IllegalStateException if this argument already has an executor
     */
    @SuppressWarnings("unchecked")
    public final SELF executeAsync(@NotNull AsyncCommandExecutor<C> executor) {
        Objects.requireNonNull(executor);
        if (this.executor != null) {
            throw new IllegalStateException("Argument '" + name + "' already has an executor, set by execute or "
                    + "executeAsync; an argument has one executor");
        }
        this.executor = executor;
        return (SELF) this;
    }

    protected final ArgumentType<?> type() {
        return type;
    }

    public abstract T cast(Object parsedArgument);

    @SuppressWarnings("unchecked")
    public final SELF validator(@NotNull Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        this.validator = (x, ctx) -> {
            if (!predicate.test(x)) {
                throw ArgumentValidationException.ofIncorrectInput(name(), ctx, ctx.getInput(name()));
            }
        };
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public final SELF validator(@NotNull BiFunction<? super T, C, Boolean> predicate) {
        Objects.requireNonNull(predicate);
        this.validator = (x, ctx) -> {
            if (!predicate.apply(x, ctx)) {
                throw ArgumentValidationException.ofIncorrectInput(name(), ctx, ctx.getInput(name()));
            }
        };
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public final SELF validator(@NotNull ArgumentValidator<? super T, C> validator) {
        this.validator = Objects.requireNonNull(validator);
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public final SELF transformer(@NotNull Function<? super T, ? extends T> transformer) {
        Objects.requireNonNull(transformer);
        this.transformer = (x, ctx) -> transformer.apply(x);
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public final SELF transformer(@NotNull BiFunction<? super T, C, ? extends T> transformer) {
        this.transformer = Objects.requireNonNull(transformer);
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public final SELF addUncaughtExceptionHandler(@NotNull UncaughtExceptionHandler<C> handler) {
        this.uncaughtExceptionHandlers.add(handler);
        return (SELF) this;
    }

    @NotNull
    protected final Predicate<? super T> filter(C ctx) {
        ArgumentValidator<? super T, C> validator = this.validator;
        return (x) -> {
            try {
                validator.validate(x, ctx);
                return true;
            } catch (ArgumentParseException e) {
                return false;
            }
        };
    }

    @NotNull
    final T parse(C ctx) throws ArgumentParseException {
        T t;
        try {
            t = Objects.requireNonNull(parseImpl(ctx));
        } catch (CommandSyntaxException e) {
            t = additionalParser.apply(ctx, ctx.getInput(name));
            if (t == null) {
                throw ArgumentParseException.ofCommandSyntaxException(e);
            }
        } catch (ArgumentParseException e) {
            t = additionalParser.apply(ctx, ctx.getInput(name));
            if (t == null) {
                throw e;
            }
        }

        validator.validate(t, ctx);
        return transformer.apply(t, ctx);
    }

    /**
     * Parses the value of the argument node {@code nodeName}, such as the value node of an option, as if it were this
     * argument's own node.
     */
    final T parseNode(C ctx, String nodeName) throws ArgumentParseException {
        return parseNode(ctx, (CommonCommandContext<?, ?>) ctx, nodeName);
    }

    // sameCtx is ctx, typed to name its source type.
    private <S> T parseNode(C ctx, CommonCommandContext<S, ?> sameCtx, String nodeName) throws ArgumentParseException {
        CommandContext<S> handle = sameCtx.getHandle();
        ParsedCommandNode<S> node = null;
        for (ParsedCommandNode<S> x : handle.getNodes()) {
            if (x.getNode() instanceof ArgumentCommandNode && x.getNode()
                                                               .getName()
                                                               .equals(nodeName)) {
                node = x;
            }
        }
        if (node == null) {
            throw new IllegalStateException("No argument node " + nodeName + " in the context");
        }
        StringRange range = node.getRange();
        Object value = handle.getArgument(nodeName, Object.class);
        // The argument reads its value from the handle by its own name, so it gets a handle with only this value, on
        // a node of that name, which an error message looks up to point at the value.
        CommandContext<S> valueHandle = new CommandContext<>(handle.getSource(),
                                                             handle.getInput(),
                                                             Map.of(name,
                                                                    new ParsedArgument<>(range.getStart(),
                                                                                         range.getEnd(),
                                                                                         value)),
                                                             handle.getCommand(),
                                                             handle.getRootNode(),
                                                             List.of(new ParsedCommandNode<>(argumentNode(name, type()),
                                                                                             range)),
                                                             range,
                                                             null,
                                                             handle.getRedirectModifier(),
                                                             handle.isForked());
        @Nullable
        String input = sameCtx.isArgumentGiven(name) ? sameCtx.getInput(name) : null;
        sameCtx.replaceHandle(valueHandle, name, range.get(handle.getInput()));
        try {
            return parse(ctx);
        } finally {
            sameCtx.replaceHandle(handle, name, input);
        }
    }

    private static <S, V> ArgumentCommandNode<S, V> argumentNode(String name, ArgumentType<V> type) {
        return RequiredArgumentBuilder.<S, V>argument(name, type)
                                      .build();
    }

    protected abstract T parseImpl(C ctx) throws CommandSyntaxException, ArgumentParseException;
}
