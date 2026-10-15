package dev.marulabs.commandlib;

import dev.marulabs.commandlib.command.CommandMiddleware;
import dev.marulabs.commandlib.command.Extractor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Middlewares and context value providers for every command of a registration, given to {@code CommandLib.register}
 * or to a tester's builder:
 *
 * <pre>{@code
 * CommandDefaults<CommandContext> defaults = CommandDefaults.of(CommandContext.class)
 *         .addMiddleware(audit)
 *         .provide(GamePlayer.class, ctx -> games.of(Command.playerRequirer().extract(ctx)));
 * CommandLib.register(this, defaults, List.of(new TeamCommand(), new ArenaCommand()));
 * }</pre>
 *
 * The middlewares run before those of each command, outermost first, before its cooldown and confirmation, and also
 * for a child command that calls {@code disableParentMiddleware()}, since they belong to the registration rather than
 * to a parent. A provider gives its value unless the command or one of its parents provides the same key.
 * <p>
 * The registration keeps the defaults on its top-level commands, so registering a command again replaces them.
 *
 * @param <C> the platform's context
 */
public final class CommandDefaults<C extends CommonCommandContext<?, ?>> {
    private final List<CommandMiddleware<? super C>> middlewares = new ArrayList<>();
    private final Map<ContextKey<?>, Extractor<? super C, ?>> providers = new LinkedHashMap<>();

    private CommandDefaults() {
    }

    /**
     * Creates empty defaults for commands of {@code contextType}, the platform's {@code CommandContext}.
     */
    @NotNull
    public static <C extends CommonCommandContext<?, ?>> CommandDefaults<C> of(@NotNull Class<C> contextType) {
        Objects.requireNonNull(contextType);
        return new CommandDefaults<>();
    }

    /**
     * Adds a middleware that wraps the execution of every command, as {@link CommonCommand#addMiddleware} does.
     * Middlewares run in the order they are added.
     */
    @NotNull
    public CommandDefaults<C> addMiddleware(@NotNull CommandMiddleware<? super C> middleware) {
        middlewares.add(Objects.requireNonNull(middleware));
        return this;
    }

    /**
     * Provides the value of {@code key} to every command, as {@link CommonCommand#provide(ContextKey, Extractor)}
     * does, unless the command provides the key itself.
     */
    @NotNull
    public <V> CommandDefaults<C> provide(@NotNull ContextKey<V> key,
                                          @NotNull Extractor<? super C, ? extends V> provider) {
        providers.put(Objects.requireNonNull(key), Objects.requireNonNull(provider));
        return this;
    }

    /**
     * Same as {@link #provide(ContextKey, Extractor)} with the key of {@code type}.
     */
    @NotNull
    public <V> CommandDefaults<C> provide(@NotNull Class<V> type, @NotNull Extractor<? super C, ? extends V> provider) {
        return provide(ContextKey.of(type), provider);
    }

    /**
     * Sets these defaults on each top-level command of a registration.
     */
    void applyTo(Collection<? extends CommonCommand<C, ?>> commands) {
        commands.forEach(x -> x.defaults(this));
    }

    List<CommandMiddleware<? super C>> middlewares() {
        return List.copyOf(middlewares);
    }

    Map<ContextKey<?>, Extractor<? super C, ?>> providers() {
        return Map.copyOf(providers);
    }
}
