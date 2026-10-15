package dev.marulabs.commandlib;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The requirement of an option node. It lets everyone use the node and tells the runner what the node gives.
 * Platforms keep a node's requirement when they copy or convert the node, as Paper's dispatcher mirror does, so the
 * requirement identifies an option node where the node object itself does not.
 */
final class OptionNode<S> implements Predicate<S> {
    private final OptionGraph<S, ?> graph;
    private final List<? extends CommandOption<?, ?>> flags;
    private final @Nullable CommandOption<?, ?> value;
    private final @Nullable Map<? extends CommandOption<?, ?>, Integer> state;

    OptionNode(OptionGraph<S, ?> graph,
               List<? extends CommandOption<?, ?>> flags,
               @Nullable CommandOption<?, ?> value,
               @Nullable Map<? extends CommandOption<?, ?>, Integer> state) {
        this.graph = graph;
        this.flags = List.copyOf(flags);
        this.value = value;
        this.state = state;
    }

    @Override
    public boolean test(S source) {
        return true;
    }

    OptionGraph<S, ?> graph() {
        return graph;
    }

    /**
     * The flags a literal token gives, one entry per occurrence; empty for the literal of a value option.
     */
    List<? extends CommandOption<?, ?>> flags() {
        return flags;
    }

    /**
     * The option whose value an argument node holds, or null for a literal token.
     */
    @Nullable
    CommandOption<?, ?> value() {
        return value;
    }

    /**
     * The option counts after the node, or null for the literal of a value option, which its value follows.
     */
    @Nullable
    Map<? extends CommandOption<?, ?>, Integer> state() {
        return state;
    }
}
