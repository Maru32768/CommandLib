package dev.marulabs.commandlib;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    // Each set of options given offers every order of the short flags left, so the tree grows exponentially.
    @Test
    void a_command_whose_options_make_too_many_nodes_is_rejected_at_registration() {
        assertThat(nodes(flags(7))).isLessThan(40_000);
        assertThatThrownBy(() -> build(flags(8))).isInstanceOf(IllegalStateException.class)
                                                .hasMessageContaining("'flags'")
                                                .hasMessageContaining("options.md");
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

    @Test
    void options_after_the_arguments_grow_the_tree_with_the_arguments_only() {
        TestCommand command = benchmarkCommand();
        command.optionsAnywhere();
        // The 8 states each copy target and count, which offer the tokens of their state again (133 nodes). The
        // copies redirect like the tokens, so a player gets each node once.
        assertThat(nodes(command)).isLessThan(150);
        assertThat(sent(command)).isEqualTo(nodes(command));
        // One argument: the tokens of each state before and after it, twice the 187 nodes of flags(4) (386).
        assertThat(nodes(anywhere(flags(4)))).isLessThan(2 * nodes(flags(4)) + 20);
        assertThat(sent(anywhere(flags(4)))).isLessThan(400);
        assertThat(sent(anywhere(flags(6)))).isLessThan(12_000);
    }

    @Test
    void every_redirect_follows_its_target_in_the_per_player_copy() {
        TestCommand restricted = flags(4);
        restricted.optionsAnywhere();
        restricted.option(Options.flag("secret", 'z')
                                 .permission(DefaultPermission.OP));
        for (TestCommand command : List.of(benchmarkCommand(), anywhere(benchmarkCommand()), restricted)) {
            for (boolean operator : List.of(true, false)) {
                Set<CommandNode<?>> copied = Collections.newSetFromMap(new IdentityHashMap<>());
                TestCommandSource source = new TestCommandSource(operator ? "test.flags.option.secret" : "none") {
                    @Override
                    public boolean hasPermission(String permission) {
                        return !permission.endsWith(".option.secret") || super.hasPermission(permission);
                    }
                };
                for (CommandNode<TestCommandSource> node : build(command)) {
                    copy(node, source, copied);
                }
            }
        }
    }

    // Copies the tree as vanilla's Commands#fillUsableCommands does, which resolves a redirect to the copy of its
    // target made so far.
    private static void copy(CommandNode<TestCommandSource> node,
                             TestCommandSource source,
                             Set<CommandNode<?>> copied) {
        copied.add(node);
        for (CommandNode<TestCommandSource> child : node.getChildren()) {
            if (!child.canUse(source)) {
                continue;
            }
            if (child.getRedirect() != null) {
                assertThat(copied).as("the target of %s", child.getName())
                                  .contains(child.getRedirect());
            }
            copy(child, source, copied);
        }
    }

    private static TestCommand anywhere(TestCommand command) {
        command.optionsAnywhere();
        return command;
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

    private static List<LiteralCommandNode<TestCommandSource>> build(TestCommand command) {
        return new CommandNodeCreator<TestCommandSource, String, TestCommandContext, TestCommand>(List.of(command),
                                                                                                  "test",
                                                                                                  TestCommandRunner.INLINE).build();
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
