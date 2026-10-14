package dev.marulabs.commandlib;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An argument that takes another argument one or more times, made with {@link CommonArgument#variadic()}.
 *
 * <p>The command tree repeats the element's node under the same name, so a client shows {@code <targets>} for each
 * value. Brigadier keeps only the last value of a name, so each value is parsed again from its own token with the
 * element's argument type, then read by the element from a handle holding only that value.
 *
 * @param <T> the type of each value
 * @param <C> the command context type
 */
public final class VariadicArgument<T, C extends CommonCommandContext<?, ?>> extends CommonArgument<List<T>, C, VariadicArgument<T, C>> {
    /**
     * The number of values {@link CommonArgument#variadic()} accepts.
     */
    public static final int DEFAULT_MAX_COUNT = 16;

    private final CommonArgument<T, C, ?> element;
    private final int maxCount;

    VariadicArgument(@NotNull CommonArgument<T, C, ?> element, int maxCount) {
        super(element.name(), element.type());
        if (maxCount < 1) {
            throw new IllegalArgumentException("maxCount must be at least 1: " + maxCount);
        }
        this.element = Objects.requireNonNull(element);
        this.maxCount = maxCount;
        // Each node takes one value, so it suggests what the element suggests.
        copySuggestionProviders(element);
    }

    @Override
    int nodeCount() {
        return maxCount;
    }

    @Override
    @SuppressWarnings("unchecked") // parseImpl stores a List<T> under this argument's name.
    public List<T> cast(Object parsedArgument) {
        return (List<T>) parsedArgument;
    }

    @Override
    protected List<T> parseImpl(C ctx) throws ArgumentParseException {
        List<T> values = new ArrayList<>();
        CommonCommandContext<?, ?> sameCtx = ctx;
        parseValues(ctx, sameCtx, values);
        return List.copyOf(values);
    }

    // sameCtx is ctx, typed to name its source type.
    private <S> void parseValues(C ctx,
                                 CommonCommandContext<S, ?> sameCtx,
                                 List<T> values) throws ArgumentParseException {
        CommandContext<S> handle = sameCtx.getHandle();
        String input = sameCtx.getInput(name());
        for (CommandContext<S> context = handle; context != null; context = context.getChild()) {
            for (ParsedCommandNode<S> node : context.getNodes()) {
                if (!(node.getNode() instanceof ArgumentCommandNode) || !node.getNode()
                                                                             .getName()
                                                                             .equals(name())) {
                    continue;
                }
                // The element reads its value from the handle by name, so it gets a handle with only this value.
                sameCtx.replaceHandle(valueHandle(context, node),
                                      name(),
                                      node.getRange()
                                          .get(context.getInput()));
                try {
                    values.add(element.parse(ctx));
                } finally {
                    sameCtx.replaceHandle(handle, name(), input);
                }
            }
        }
    }

    private <S> CommandContext<S> valueHandle(CommandContext<S> handle,
                                              ParsedCommandNode<S> node) throws ArgumentParseException {
        StringRange range = node.getRange();
        StringReader reader = new StringReader(handle.getInput());
        reader.setCursor(range.getStart());
        Object value;
        try {
            value = type().parse(reader);
        } catch (CommandSyntaxException e) {
            throw PlatformAdapter.get()
                                 .convertCommandSyntaxException(e);
        }
        return new CommandContext<>(handle.getSource(),
                                    handle.getInput(),
                                    Map.of(name(), new ParsedArgument<>(range.getStart(), range.getEnd(), value)),
                                    handle.getCommand(),
                                    handle.getRootNode(),
                                    List.of(node),
                                    range,
                                    null,
                                    handle.getRedirectModifier(),
                                    handle.isForked());
    }
}
