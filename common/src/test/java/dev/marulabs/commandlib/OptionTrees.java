package dev.marulabs.commandlib;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Walks the Brigadier nodes {@link OptionGraph} builds for a command's options.
 */
final class OptionTrees {
    private OptionTrees() {
    }

    /**
     * The nodes under {@code node} that redirect, without following redirects.
     */
    static <S> List<CommandNode<S>> redirects(CommandNode<S> node) {
        List<CommandNode<S>> redirects = new ArrayList<>();
        Deque<CommandNode<S>> queue = new ArrayDeque<>(node.getChildren());
        while (!queue.isEmpty()) {
            CommandNode<S> current = queue.poll();
            if (current.getRedirect() != null) {
                redirects.add(current);
            }
            queue.addAll(current.getChildren());
        }
        return redirects;
    }

    /**
     * Asserts that every redirect under {@code command} leads to a node under {@code command} that does not redirect
     * itself and is reached by the same options: the same number of each option given before it, as the option tokens
     * on the path from {@code command} count them. {@code names} maps each long name ({@code force}) and short name
     * ({@code f}) to the option's name.
     *
     * @return the number of redirects checked
     */
    static <S> int assertRedirectsReachTheSameOptions(CommandNode<S> command, Map<String, String> names) {
        IdentityHashMap<CommandNode<S>, Map<String, Integer>> optionsBefore = new IdentityHashMap<>();
        Deque<CommandNode<S>> queue = new ArrayDeque<>();
        optionsBefore.put(command, Map.of());
        queue.add(command);
        while (!queue.isEmpty()) {
            CommandNode<S> node = queue.poll();
            for (CommandNode<S> child : node.getChildren()) {
                // The command's argument nodes follow every count of options, so only their first path is kept. The
                // option tokens, and so every redirect target, have one parent.
                if (optionsBefore.putIfAbsent(child, after(optionsBefore.get(node), child, names)) == null) {
                    queue.add(child);
                }
            }
        }

        List<CommandNode<S>> redirects = redirects(command);
        for (CommandNode<S> redirect : redirects) {
            CommandNode<S> target = redirect.getRedirect();
            assertThat(optionsBefore).as("the target of %s is a node of the command", redirect.getName())
                                     .containsKey(target);
            assertThat(target.getRedirect()).as("the target of %s redirects again", redirect.getName())
                                            .isNull();
            assertThat(optionsBefore.get(target)).as("options before %s and before its target %s",
                                                     redirect.getName(),
                                                     target.getName())
                                                 .isEqualTo(optionsBefore.get(redirect));
        }
        return redirects.size();
    }

    // The options given once the node is read: an option token such as "--force", "-f" or "-fv" adds its options.
    private static Map<String, Integer> after(Map<String, Integer> before,
                                              CommandNode<?> node,
                                              Map<String, String> names) {
        String token = node.getName();
        if (!(node instanceof LiteralCommandNode) || !token.startsWith("-")) {
            return before;
        }
        Map<String, Integer> after = new TreeMap<>(before);
        List<String> given = new ArrayList<>();
        if (token.startsWith("--")) {
            given.add(token.substring(2));
        } else {
            token.substring(1)
                 .chars()
                 .forEach(x -> given.add(String.valueOf((char) x)));
        }
        for (String name : given) {
            String option = names.get(name);
            assertThat(option).as("option of %s", token)
                              .isNotNull();
            after.merge(option, 1, Integer::sum);
        }
        return after;
    }
}
