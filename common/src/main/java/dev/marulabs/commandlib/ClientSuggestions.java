package dev.marulabs.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Completes a partial input as a client does. Brigadier's server-side
 * {@link CommandDispatcher#getCompletionSuggestions(ParseResults)} asks every child of the node before the cursor, even
 * one whose requirement fails for the source, such as a literal hidden by a permission. The client's command tree
 * leaves those nodes out, so the testing artifacts complete through this instead.
 */
final class ClientSuggestions {
    private ClientSuggestions() {
    }

    static <S> CompletableFuture<Suggestions> complete(ParseResults<S> parse) {
        // CommandDispatcher#getCompletionSuggestions with the cursor at the end, plus the canUse check.
        String input = parse.getReader()
                            .getString();
        int cursor = input.length();
        CommandContextBuilder<S> context = parse.getContext();
        SuggestionContext<S> nodeBeforeCursor = context.findSuggestionContext(cursor);
        int start = Math.min(nodeBeforeCursor.startPos, cursor);
        List<CompletableFuture<Suggestions>> futures = new ArrayList<>();
        for (CommandNode<S> node : nodeBeforeCursor.parent.getChildren()) {
            if (!node.canUse(context.getSource())) {
                continue;
            }
            CompletableFuture<Suggestions> future = Suggestions.empty();
            try {
                future = node.listSuggestions(context.build(input), new SuggestionsBuilder(input, start));
            } catch (CommandSyntaxException ignored) {
                // As in Brigadier: a node that cannot suggest adds nothing.
            }
            futures.add(future);
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))
                                .thenApply(x -> Suggestions.merge(input,
                                                                  futures.stream()
                                                                         .map(CompletableFuture::join)
                                                                         .collect(Collectors.toList())));
    }
}
