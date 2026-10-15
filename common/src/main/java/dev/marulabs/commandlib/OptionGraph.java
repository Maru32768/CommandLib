package dev.marulabs.commandlib;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The option nodes of one command. Every option node built for the command has a requirement that names this graph
 * ({@link OptionNode}), so the runner counts the command's own option tokens and not those of a parent command on
 * the same path.
 */
final class OptionGraph<S, C extends CommonCommandContext<S, ?>> {
    private final List<CommandOption<?, C>> options;

    OptionGraph(List<CommandOption<?, C>> options) {
        this.options = List.copyOf(options);
    }

    List<CommandOption<?, C>> options() {
        return options;
    }

    Predicate<S> literal(List<CommandOption<?, C>> flags) {
        return new OptionNode<>(this, flags, null);
    }

    Predicate<S> value(CommandOption<?, C> option) {
        return new OptionNode<>(this, List.of(), option);
    }

    /**
     * The occurrences of each option in the parsed nodes, in input order: the internal argument name of each value,
     * and the token of each flag once per occurrence.
     */
    @SuppressWarnings("unchecked")
    Map<CommandOption<?, C>, List<String>> occurrences(CommandContext<S> handle) {
        Map<CommandOption<?, C>, List<String>> occurrences = new LinkedHashMap<>();
        for (ParsedCommandNode<S> parsed : handle.getNodes()) {
            Predicate<S> requirement = parsed.getNode()
                                             .getRequirement();
            if (!(requirement instanceof OptionNode) || ((OptionNode<S>) requirement).graph() != this) {
                continue;
            }
            OptionNode<S> node = (OptionNode<S>) requirement;
            String name = parsed.getNode()
                                .getName();
            for (CommandOption<?, ?> flag : node.flags()) {
                occurrences.computeIfAbsent((CommandOption<?, C>) flag, x -> new ArrayList<>())
                           .add(name);
            }
            CommandOption<?, ?> value = node.value();
            if (value != null) {
                occurrences.computeIfAbsent((CommandOption<?, C>) value, x -> new ArrayList<>())
                           .add(name);
            }
        }
        return occurrences;
    }
}
