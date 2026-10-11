package net.kunmc.lab.commandlib;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.argument.ArgumentValidator;
import net.kunmc.lab.commandlib.command.CommandExecutor;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.kunmc.lab.commandlib.exception.ArgumentValidationException;
import net.kunmc.lab.commandlib.suggestion.SuggestionAction;
import net.kunmc.lab.commandlib.util.UncaughtExceptionHandler;
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
    private BiFunction<C, String, T> additionalParser;
    private CommandExecutor<C> executor;
    private final ArgumentType<?> type;
    private ArgumentValidator<? super T, C> validator;
    private BiFunction<? super T, C, ? extends T> transformer;
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

    @SuppressWarnings("unchecked")
    public final SELF additionalParser(@Nullable BiFunction<C, String, T> parser) {
        this.additionalParser = parser;
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
    public final SELF execute(@Nullable CommandExecutor<C> executor) {
        this.executor = executor;
        return (SELF) this;
    }

    protected final ArgumentType<?> type() {
        return type;
    }

    public abstract T cast(Object parsedArgument);

    @SuppressWarnings("unchecked")
    public final SELF validator(@Nullable Predicate<? super T> predicate) {
        if (predicate == null) {
            this.validator = null;
        } else {
            this.validator = (x, ctx) -> {
                if (!predicate.test(x)) {
                    throw ArgumentValidationException.ofIncorrectInput(name(), ctx, ctx.getInput(name()));
                }
            };
        }
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public final SELF validator(@Nullable BiFunction<? super T, C, Boolean> predicate) {
        if (predicate == null) {
            this.validator = null;
        } else {
            this.validator = (x, ctx) -> {
                if (!predicate.apply(x, ctx)) {
                    throw ArgumentValidationException.ofIncorrectInput(name(), ctx, ctx.getInput(name()));
                }
            };
        }
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public final SELF validator(@Nullable ArgumentValidator<? super T, C> validator) {
        this.validator = validator;
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public final SELF transformer(@Nullable Function<? super T, ? extends T> transformer) {
        if (transformer == null) {
            this.transformer = null;
        } else {
            this.transformer = (x, ctx) -> transformer.apply(x);
        }
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public final SELF transformer(@Nullable BiFunction<? super T, C, ? extends T> transformer) {
        this.transformer = transformer;
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public final SELF addUncaughtExceptionHandler(@NotNull UncaughtExceptionHandler<C> handler) {
        this.uncaughtExceptionHandlers.add(handler);
        return (SELF) this;
    }

    @NotNull
    protected final Predicate<? super T> filter(C ctx) {
        if (validator == null) {
            return (x) -> true;
        }
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
            if (additionalParser != null) {
                t = additionalParser.apply(ctx, ctx.getInput(name));
            }
            if (t == null) {
                throw PlatformAdapter.get()
                                     .convertCommandSyntaxException(e);
            }
        } catch (ArgumentParseException e) {
            if (additionalParser != null) {
                t = additionalParser.apply(ctx, ctx.getInput(name));
            }
            if (t == null) {
                throw e;
            }
        }

        if (validator != null) {
            validator.validate(t, ctx);
        }

        if (transformer == null) {
            return t;
        }
        return transformer.apply(t, ctx);
    }

    protected abstract T parseImpl(C ctx) throws CommandSyntaxException, ArgumentParseException;
}
