package dev.marulabs.commandlib.internal;

import dev.marulabs.commandlib.Captions;
import dev.marulabs.commandlib.CommandScheduler;
import dev.marulabs.commandlib.ContextKey;
import dev.marulabs.commandlib.command.Extractor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * What a context gets from the command it belongs to when it is created, which it keeps unchanged. An execution has
 * all of it; a context for suggestions or a visibility predicate has the providers of the command; a context made
 * without a command, as a test makes one, has none of it.
 */
@ApiStatus.Internal
public final class ContextState {
    public static final ContextState EMPTY = new ContextState(null, null, null, Map.of());

    private final @Nullable CommandScheduler scheduler;
    private final @Nullable Object parse;
    private final @Nullable Captions captions;
    private final Map<ContextKey<?>, ? extends Extractor<?, ?>> providers;

    private ContextState(@Nullable CommandScheduler scheduler,
                         @Nullable Object parse,
                         @Nullable Captions captions,
                         Map<ContextKey<?>, ? extends Extractor<?, ?>> providers) {
        this.scheduler = scheduler;
        this.parse = parse;
        this.captions = captions;
        this.providers = providers;
    }

    /**
     * The state of an execution of a command: the scheduler, the parse it came from, and the command's captions and
     * providers.
     */
    public static ContextState execution(CommandScheduler scheduler,
                                         Object parse,
                                         Captions captions,
                                         Map<ContextKey<?>, ? extends Extractor<?, ?>> providers) {
        return new ContextState(Objects.requireNonNull(scheduler),
                                Objects.requireNonNull(parse),
                                Objects.requireNonNull(captions),
                                Map.copyOf(providers));
    }

    /**
     * The state of a context that runs code of a command outside an execution, such as its suggestion providers and
     * visibility predicates.
     */
    public static ContextState of(Map<ContextKey<?>, ? extends Extractor<?, ?>> providers) {
        return new ContextState(null, null, null, Map.copyOf(providers));
    }

    /**
     * The scheduler, or null outside an execution.
     */
    @Nullable
    public CommandScheduler scheduler() {
        return scheduler;
    }

    /**
     * The parse the execution came from, which the targets of one forked execution share, or null outside an
     * execution.
     */
    @Nullable
    public Object parse() {
        return parse;
    }

    /**
     * The captions of the command, or null for the default ones.
     */
    @Nullable
    public Captions captions() {
        return captions;
    }

    public Map<ContextKey<?>, ? extends Extractor<?, ?>> providers() {
        return providers;
    }
}
