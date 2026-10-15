package dev.marulabs.commandlib;

import com.mojang.brigadier.tree.CommandNode;
import dev.marulabs.commandlib.argument.CommonIntegerArgument;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The size of the option trees. "Nodes" are the distinct node objects the server keeps. "Sent" are the nodes a server
 * builds for each player, since vanilla's Commands#sendCommands copies a node for every path that reaches it, as
 * {@link #sent} does; a redirect is sent as a reference to its target.
 */
class OptionTreeSizeTest {
    @Test
    void short_flags_do_not_multiply_the_tree_by_their_orders() {
        assertThat(nodes(flags(6))).isLessThan(6_000);
        assertThat(sent(flags(6))).isLessThan(6_000);
        assertThat(sent(flags(4))).isLessThan(250);
    }

    @Test
    void option_states_share_the_argument_nodes() {
        // 3 options in 8 states, the arguments (target, count, list, words x4, admin) once.
        assertThat(nodes(benchmarkCommand())).isLessThan(50);
    }

    @Test
    void counts_and_repeatable_options_stay_bounded() {
        TestCommand command = new TestCommand("tag") {{
            option(Options.count("verbose", 'v', 3));
            option(Options.string("tag", 't', "")
                          .repeatable(5));
            option(Options.flag("force", 'f'));
            argument(new Word("target")).execute((target, ctx) -> {
            });
        }};

        assertThat(sent(command)).isLessThan(1_000);
    }

    static int nodes(TestCommand command) {
        Set<CommandNode<?>> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<CommandNode<?>> queue = new ArrayDeque<>(build(command));
        while (!queue.isEmpty()) {
            CommandNode<?> node = queue.poll();
            if (!seen.add(node)) {
                continue;
            }
            queue.addAll(node.getChildren());
            if (node.getRedirect() != null) {
                queue.add(node.getRedirect());
            }
        }
        return seen.size();
    }

    static int sent(TestCommand command) {
        int count = 0;
        for (CommandNode<?> node : build(command)) {
            count += sent(node);
        }
        return count;
    }

    private static int sent(CommandNode<?> node) {
        int count = 1;
        for (CommandNode<?> child : node.getChildren()) {
            count += sent(child);
        }
        return count;
    }

    private static List<? extends CommandNode<?>> build(TestCommand command) {
        return new CommandNodeCreator<>(List.of(command), "test", TestCommandRunner.INLINE).build();
    }

    static TestCommand flags(int count) {
        TestCommand command = new TestCommand("flags");
        for (int i = 0; i < count; i++) {
            command.option(Options.flag("flag" + i, (char) ('a' + i)));
        }
        command.argument(new Word("target"))
               .execute((target, ctx) -> {
               });
        return command;
    }

    // The shape of the benchmark's Trees.command.
    static TestCommand benchmarkCommand() {
        TestCommand command = new TestCommand("cmd");
        command.option(Options.flag("force", 'f'));
        command.option(Options.flag("verbose", 'v'));
        command.option(Options.integer("limit", 'n', 10, 1, 100));
        command.argument(new Word("target"), new Int("count").optional(1))
               .execute((target, count, ctx) -> {
               });
        TestCommand list = new TestCommand("list");
        list.argument(new Word("words").variadic(4))
            .execute((words, ctx) -> {
            });
        TestCommand admin = new TestCommand("admin");
        admin.execute(ctx -> {
        });
        command.addChildren(list, admin);
        return command;
    }

    private static final class Word extends CommonStringArgument<TestCommandContext, Word> {
        Word(String name) {
            super(name, Type.WORD);
        }
    }

    private static final class Int extends CommonIntegerArgument<TestCommandContext, Int> {
        Int(String name) {
            super(name);
        }
    }
}
