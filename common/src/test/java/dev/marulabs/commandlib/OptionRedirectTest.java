package dev.marulabs.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Options that loop through redirects: Brigadier executes the context after the last option, and the runner reads
 * the options before it from the input again.
 */
class OptionRedirectTest {
    private static TestCommand scan() {
        return new TestCommand("scan") {{
            addAliases("sc");
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Integer, TestCommandContext> verbose = option(Options.count("verbose", 'v', 3));
            CommandOption<Integer, TestCommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100));
            CommandOption<List<String>, TestCommandContext> tags = option(Options.string("tag", 't', "")
                                                                                 .repeatable(3));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + " force=" + ctx.getOption(force) + " verbose=" + ctx.getOption(verbose)
                        + " limit=" + ctx.getOption(limit) + " tags=" + ctx.getOption(tags) + " inputs="
                        + ctx.getInputs());
            });
            execute(ctx -> ctx.sendMessage("bare force=" + ctx.getOption(force) + " verbose="
                    + ctx.getOption(verbose) + " limit=" + ctx.getOption(limit) + " tags=" + ctx.getOption(tags)));
        }};
    }

    @Test
    void the_tree_redirects_tokens_that_reach_the_same_options() {
        TestCommandRunner runner = new TestCommandRunner(scan());
        List<CommandNode<TestCommandSource>> redirects = new java.util.ArrayList<>();
        Deque<CommandNode<TestCommandSource>> queue = new ArrayDeque<>(runner.root()
                                                                             .getChildren());
        while (!queue.isEmpty()) {
            CommandNode<TestCommandSource> node = queue.poll();
            if (node.getRedirect() != null) {
                redirects.add(node);
            }
            queue.addAll(node.getChildren());
        }

        assertThat(redirects).isNotEmpty();
    }

    @Test
    void options_before_the_last_redirect_are_read_in_input_order() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(scan());

        assertThat(runner.execute("scan -t red -fv --limit 20 -t blue --tag green -v Alex")
                         .messages()).containsExactly("Alex force=true verbose=2 limit=20 tags=[red, blue, green] inputs=[scan, Alex]");
        assertThat(runner.execute("scan -vf -n 5 Alex")
                         .messages()).containsExactly("Alex force=true verbose=1 limit=5 tags=[] inputs=[scan, Alex]");
        assertThat(runner.execute("scan Alex")
                         .messages()).containsExactly("Alex force=false verbose=0 limit=10 tags=[] inputs=[scan, Alex]");
    }

    @Test
    void input_that_ends_with_options_runs_the_command() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(scan());

        assertThat(runner.execute("scan -v -f")
                         .messages()).containsExactly("bare force=true verbose=1 limit=10 tags=[]");
        assertThat(runner.execute("scan --force -t a -n 3")
                         .messages()).containsExactly("bare force=true verbose=0 limit=3 tags=[a]");
    }

    @Test
    void a_space_after_the_last_option_is_rejected_as_before() {
        TestCommandRunner runner = new TestCommandRunner(scan());

        // Brigadier leaves the space unread after --force, which holds the nodes after {force}.
        assertThatThrownBy(() -> runner.execute("scan --force ")).isInstanceOf(CommandSyntaxException.class)
                                                                 .hasMessageContaining("Incorrect argument");
        // -f redirects to --force; Brigadier follows the redirect and the word argument parses the empty rest.
        assertThatThrownBy(() -> runner.execute("scan -f ")).isInstanceOf(CommandSyntaxException.class)
                                                            .hasMessageContaining("Incorrect argument")
                                                            .hasMessageContaining("scan -f<--[HERE]");
        assertThatThrownBy(() -> runner.execute("scan -n 5 ")).isInstanceOf(CommandSyntaxException.class)
                                                              .hasMessageContaining("Incorrect argument");
    }

    @Test
    void an_option_value_may_look_like_an_option() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(scan());

        assertThat(runner.execute("scan -t -f -f -t -v Alex")
                         .messages()).containsExactly("Alex force=true verbose=0 limit=10 tags=[-f, -v] inputs=[scan, Alex]");
    }

    @Test
    void options_are_read_after_an_alias_a_leading_slash_and_a_redirect_before_the_command() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(scan());
        runner.redirectToRoot("run");

        assertThat(runner.execute("sc -f -v Alex")
                         .messages()).containsExactly("Alex force=true verbose=1 limit=10 tags=[] inputs=[sc, Alex]");
        assertThat(runner.executeWithLeadingSlash("/scan -f -v Alex")
                         .messages()).containsExactly("Alex force=true verbose=1 limit=10 tags=[] inputs=[scan, Alex]");
        assertThat(runner.execute("run scan -f -n 7 Alex")
                         .messages()).containsExactly("Alex force=true verbose=0 limit=7 tags=[] inputs=[scan, Alex]");
        assertThat(runner.execute("run run scan -v -f")
                         .messages()).containsExactly("bare force=true verbose=1 limit=10 tags=[]");
    }

    @Test
    void options_are_read_for_a_child_command() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            addChildren(new TestCommand("start") {{
                CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
                CommandOption<Boolean, TestCommandContext> quiet = option(Options.flag("quiet", 'q'));
                argument(new CommonStringArgument<>("arena")).execute((arena, ctx) -> ctx.sendMessage(arena
                        + " " + ctx.getOption(force) + " " + ctx.getOption(quiet) + " " + ctx.getInputs()));
            }});
        }});

        assertThat(runner.execute("game start -q -f arena1")
                         .messages()).containsExactly("arena1 true true [game, start, arena1]");
    }

    @Test
    void the_limits_of_the_options_stay_in_the_tree() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            option(Options.count("verbose", 'v', 2));
            option(Options.integer("limit", 'n', 10));
            execute(ctx -> {
            });
        }});

        assertThatThrownBy(() -> runner.execute("scan -v -n 1 -v -v")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("scan -n 1 -v -n 2")).isInstanceOf(CommandSyntaxException.class);
        assertThat(runner.suggest("scan -v -n 1 -")).containsExactlyInAnyOrder("-v", "--verbose");
    }

    @Test
    void suggestions_after_options_list_the_remaining_tokens_and_the_argument() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            option(Options.flag("force", 'f'));
            option(Options.flag("quiet", 'q'));
            argument(new Word("target").addSuggestionProvider(sb -> sb.suggest("Alex"))).execute((target, ctx) -> {
            });
        }});

        assertThat(runner.suggest("scan -q -f ")).containsExactly("Alex");
        assertThat(runner.suggest("scan -f -")).contains("-q", "--quiet")
                                               .doesNotContain("-f", "--force");
        assertThat(runner.suggest("scan -f A")).containsExactly("Alex");
    }

    private static final class Word extends CommonStringArgument<TestCommandContext, Word> {
        Word(String name) {
            super(name);
        }
    }
}
