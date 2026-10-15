package dev.marulabs.commandlib;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.tree.ArgumentCommandNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A command context that reads the nodes and arguments of several contexts as one. Brigadier starts a new context
 * after each redirect, such as {@code execute run} or a Paper label that redirects to {@code <plugin>:<label>}, and
 * an argument of an earlier context is not readable from a later one. Brigadier's context does not expose its
 * argument map, so each argument is read from the last part with an argument node of its name.
 */
final class ChainedCommandContext<S> extends CommandContext<S> {
    private final List<CommandContext<S>> parts;

    private ChainedCommandContext(List<CommandContext<S>> parts, S source) {
        super(source,
              last(parts).getInput(),
              Map.of(),
              last(parts).getCommand(),
              parts.get(0)
                   .getRootNode(),
              nodes(parts),
              StringRange.between(parts.get(0)
                                       .getRange()
                                       .getStart(),
                                  last(parts).getRange()
                                             .getEnd()),
              null,
              last(parts).getRedirectModifier(),
              last(parts).isForked());
        this.parts = List.copyOf(parts);
    }

    /**
     * The context and its children as one context, or the context itself when it has no child.
     */
    static <S> CommandContext<S> flatten(CommandContext<S> context) {
        if (context.getChild() == null) {
            return context;
        }
        List<CommandContext<S>> parts = new ArrayList<>();
        for (CommandContext<S> part = context; part != null; part = part.getChild()) {
            parts.add(part);
        }
        return new ChainedCommandContext<>(parts, last(parts).getSource());
    }

    /**
     * The given contexts as one context, with the source and command of the last one.
     */
    static <S> CommandContext<S> of(List<CommandContext<S>> parts) {
        return parts.size() == 1 ? parts.get(0) : new ChainedCommandContext<>(parts, last(parts).getSource());
    }

    private static <S> CommandContext<S> last(List<CommandContext<S>> parts) {
        return parts.get(parts.size() - 1);
    }

    private static <S> List<ParsedCommandNode<S>> nodes(List<CommandContext<S>> parts) {
        List<ParsedCommandNode<S>> nodes = new ArrayList<>();
        parts.forEach(x -> nodes.addAll(x.getNodes()));
        return nodes;
    }

    @Override
    public <V> V getArgument(String name, Class<V> clazz) {
        for (int i = parts.size() - 1; i >= 0; i--) {
            CommandContext<S> part = parts.get(i);
            if (hasArgumentNode(part, name)) {
                return part.getArgument(name, clazz);
            }
        }
        throw new IllegalArgumentException("No such argument '" + name + "' exists on this command");
    }

    private static <S> boolean hasArgumentNode(CommandContext<S> context, String name) {
        for (ParsedCommandNode<S> node : context.getNodes()) {
            if (node.getNode() instanceof ArgumentCommandNode && node.getNode()
                                                                     .getName()
                                                                     .equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public CommandContext<S> copyFor(S source) {
        if (getSource() == source) {
            return this;
        }
        List<CommandContext<S>> copies = new ArrayList<>();
        parts.forEach(x -> copies.add(x.copyFor(source)));
        return new ChainedCommandContext<>(copies, source);
    }
}
