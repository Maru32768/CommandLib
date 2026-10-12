package dev.marulabs.commandlib;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.argument.ArgumentValidator;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.exception.ArgumentValidationException;
import dev.marulabs.commandlib.suggestion.SuggestionAction;
import dev.marulabs.commandlib.util.UncaughtExceptionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class CommonArgument<T, C extends CommonCommandContext<?, ?>, SELF extends CommonArgument<T, C, SELF>> {
    private final String name;
    private final List<ArgumentSuggestionAction<C>> suggestionActions = new ArrayList<>();
    // Returns null when it cannot parse the input either.
    private BiFunction<C, String, T> additionalParser = (ctx, input) -> null;
    // Null until execute is called.
    private @Nullable CommandExecutor<C> executor;
    private final ArgumentType<?> type;
    private ArgumentValidator<? super T, C> validator = (x, ctx) -> {
    };
    private BiFunction<? super T, C, ? extends T> transformer = (x, ctx) -> x;
    private final List<UncaughtExceptionHandler<C>> uncaughtExceptionHandlers = new ArrayList<>();

    protected CommonArgument(@NotNull String name, @NotNull ArgumentType<?> type) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
        this.suggestionActions.add(ArgumentSuggestionAction.defaults());
    }

    public final String name() {
        return name;
    }

    final List<ArgumentSuggestionAction<C>> suggestionActions() {
        return List.copyOf(suggestionActions);
    }

    @SuppressWarnings("unchecked")
    public final SELF addSuggestionAction(@NotNull SuggestionAction<C> suggestionAction) {
        this.suggestionActions.add(ArgumentSuggestionAction.custom(suggestionAction));
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public final SELF setSuggestionAction(@NotNull SuggestionAction<C> suggestionAction) {
        Objects.requireNonNull(suggestionAction,
                               "suggestionAction must not be null; use clearSuggestionActions() to remove all suggestions");
        this.suggestionActions.clear();
        this.suggestionActions.add(ArgumentSuggestionAction.custom(suggestionAction));
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public final SELF clearSuggestionActions() {
        this.suggestionActions.clear();
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public final SELF useDefaultSuggestions() {
        boolean alreadyUsesDefaults = this.suggestionActions.stream()
                                                            .anyMatch(ArgumentSuggestionAction::isDefaultSuggestions);
        if (!alreadyUsesDefaults) {
            this.suggestionActions.add(ArgumentSuggestionAction.defaults());
        }
        return (SELF) this;
    }

    /**
     * Swaps the argument type's default suggestions, where they are enabled, for the given action and keeps every
     * other suggestion action in place.
     */
    protected final void replaceDefaultSuggestions(@NotNull SuggestionAction<C> suggestionAction) {
        Objects.requireNonNull(suggestionAction);
        this.suggestionActions.replaceAll(x -> x.isDefaultSuggestions()
                ? ArgumentSuggestionAction.custom(suggestionAction) : x);
    }

    @SuppressWarnings("unchecked")
    public final SELF additionalParser(@NotNull BiFunction<C, String, T> parser) {
        this.additionalParser = Objects.requireNonNull(parser);
        return (SELF) this;
    }

    @Nullable
    protected final CommandExecutor<C> executor() {
        return executor;
    }

    protected final List<UncaughtExceptionHandler<C>> uncaughtExceptionHandlers() {
        return List.copyOf(uncaughtExceptionHandlers);
    }

    @SuppressWarnings("unchecked")
    public final SELF execute(@NotNull CommandExecutor<C> executor) {
        this.executor = Objects.requireNonNull(executor);
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
        T t = null;
        try {
            t = Objects.requireNonNull(parseImpl(ctx));
        } catch (CommandSyntaxException e) {
            t = additionalParser.apply(ctx, ctx.getInput(name));
            if (t == null) {
                throw PlatformAdapter.get()
                                     .convertCommandSyntaxException(e);
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

    protected abstract T parseImpl(C ctx) throws CommandSyntaxException, ArgumentParseException;
}
