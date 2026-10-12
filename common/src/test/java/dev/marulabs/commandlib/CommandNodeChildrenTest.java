package dev.marulabs.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommandNodeChildrenTest {
    @Test
    void an_iteration_in_progress_keeps_the_old_children() {
        RootCommandNode<Object> root = new RootCommandNode<>();
        root.addChild(literal("a", 1));
        root.addChild(literal("b", 2));
        Iterator<CommandNode<Object>> iterator = root.getChildren()
                                                     .iterator();
        iterator.next();

        CommandNodeChildren.replace(root, List.of("b"), List.of(literal("c", 3)));

        List<String> rest = new ArrayList<>();
        iterator.forEachRemaining(x -> rest.add(x.getName()));
        assertThat(rest).containsExactly("b");
        assertThat(root.getChildren()).extracting(CommandNode::getName)
                                      .containsExactly("a", "c");
    }

    @Test
    void an_added_node_replaces_a_child_of_the_same_name_and_is_dispatched() throws CommandSyntaxException {
        CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        dispatcher.getRoot()
                  .addChild(LiteralArgumentBuilder.literal("a")
                                                  .executes(ctx -> 1)
                                                  .then(LiteralArgumentBuilder.literal("old"))
                                                  .build());
        LiteralCommandNode<Object> replacement = literal("a", 2);

        CommandNodeChildren.replace(dispatcher.getRoot(), List.of(), List.of(replacement));

        assertThat(dispatcher.getRoot()
                             .getChild("a")).isSameAs(replacement);
        assertThat(replacement.getChild("old")).isNull();
        assertThat(dispatcher.execute("a", new Object())).isEqualTo(2);
    }

    private static LiteralCommandNode<Object> literal(String name, int result) {
        return LiteralArgumentBuilder.literal(name)
                                     .executes(ctx -> result)
                                     .build();
    }
}
