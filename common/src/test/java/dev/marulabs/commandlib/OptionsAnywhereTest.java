package dev.marulabs.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.argument.CommonIntegerArgument;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Options that may follow the arguments ({@link CommonCommand#optionsAnywhere()}).
 */
class OptionsAnywhereTest {
    private static TestCommand ban() {
        return new TestCommand("ban") {{
            addAliases("b");
            optionsAnywhere();
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Boolean, TestCommandContext> silent = option(Options.flag("silent", 's'));
            CommandOption<Integer, TestCommandContext> days = option(Options.integer("days", 'd', 0));
            CommandOption<Boolean, TestCommandContext> json = option(Options.flag("json", 'j'));
            CommandOption<Boolean, TestCommandContext> yaml = option(Options.flag("yaml", 'y'));
            mutuallyExclusive(json, yaml);
            argument(new Word("target"), new Word("reason").optional("none")).execute((target, reason, ctx) -> {
                ctx.sendMessage(target + " reason=" + reason + " force=" + ctx.getOption(force) + " silent="
                        + ctx.getOption(silent) + " days=" + ctx.getOption(days) + " inputs=" + ctx.getInputs());
            });
            execute(ctx -> ctx.sendMessage("bare force=" + ctx.getOption(force)));
        }};
    }

    @Test
    void options_follow_the_arguments() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(ban());

        assertThat(runner.execute("ban Steve -s")
                         .messages()).containsExactly("Steve reason=none force=false silent=true days=0 inputs=[ban, Steve]");
        assertThat(runner.execute("ban Steve -s spam")
                         .messages()).containsExactly("Steve reason=spam force=false silent=true days=0 inputs=[ban, Steve, spam]");
        assertThat(runner.execute("ban Steve spam -fs")
                         .messages()).containsExactly("Steve reason=spam force=true silent=true days=0 inputs=[ban, Steve, spam]");
        assertThat(runner.execute("ban Steve spam -d 3 --force")
                         .messages()).containsExactly("Steve reason=spam force=true silent=false days=3 inputs=[ban, Steve, spam]");
    }

    @Test
    void options_still_come_before_the_arguments_and_mix_with_them() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(ban());

        assertThat(runner.execute("ban -f Steve -d 7 spam -s")
                         .messages()).containsExactly("Steve reason=spam force=true silent=true days=7 inputs=[ban, Steve, spam]");
        assertThat(runner.execute("ban -s -d 2 Steve spam")
                         .messages()).containsExactly("Steve reason=spam force=false silent=true days=2 inputs=[ban, Steve, spam]");
        assertThat(runner.execute("ban -f")
                         .messages()).containsExactly("bare force=true");
    }

    @Test
    void the_limits_count_across_the_whole_input() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(ban());

        assertThatThrownBy(() -> runner.execute("ban -s Steve spam -s")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("ban -j Steve spam -y")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("ban Steve -fs spam -f")).isInstanceOf(CommandSyntaxException.class);
        // A word argument takes a token that is no option of the state, as it takes an unknown option.
        assertThat(runner.execute("ban -s Steve -s")
                         .messages()).containsExactly("Steve reason=-s force=false silent=true days=0 inputs=[ban, Steve, -s]");
    }

    @Test
    void options_are_read_after_an_alias_a_leading_slash_and_a_redirect_before_the_command() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(ban());
        runner.redirectToRoot("run");

        assertThat(runner.execute("b Steve -s spam")
                         .messages()).containsExactly("Steve reason=spam force=false silent=true days=0 inputs=[b, Steve, spam]");
        assertThat(runner.executeWithLeadingSlash("/ban Steve -s spam")
                         .messages()).containsExactly("Steve reason=spam force=false silent=true days=0 inputs=[ban, Steve, spam]");
        assertThat(runner.execute("run ban ban -d 1 ban")
                         .messages()).containsExactly("ban reason=ban force=false silent=false days=1 inputs=[ban, ban, ban]");
    }

    @Test
    void option_tokens_are_suggested_after_the_arguments() {
        TestCommandRunner runner = new TestCommandRunner(ban());

        assertThat(runner.suggest("ban Steve -")).contains("-s", "--silent", "-f", "-d");
        assertThat(runner.suggest("ban Steve -j -")).contains("-s")
                                                    .doesNotContain("-y", "--yaml", "-j");
    }

    @Test
    void the_help_keeps_its_usage() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(ban());

        assertThat(runner.execute("ban help")
                         .messages()).anyMatch(x -> x.replaceAll("§.", "")
                                                     .contains("/ban [options] <target> [reason]"));
    }

    @Test
    void options_do_not_follow_the_values_of_a_variadic_argument() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("heal") {{
            optionsAnywhere();
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            argument(new Word("item"), new Int("amounts").variadic(3)).execute((item,
                                                                                amounts,
                                                                                ctx) -> ctx.sendMessage(item + " "
                                                                                        + amounts + " "
                                                                                        + ctx.getOption(force)));
        }});

        assertThat(runner.execute("heal a -f 1 2")
                         .messages()).containsExactly("a [1, 2] true");
        assertThat(runner.execute("heal -f a 1")
                         .messages()).containsExactly("a [1] true");
        assertThatThrownBy(() -> runner.execute("heal a 1 -f 2")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("heal a 1 2 -f")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void an_argument_child_keeps_its_parent_options_after_the_argument() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            optionsAnywhere();
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            argument(new Word("key")).execute((key, ctx) -> ctx.sendMessage(key + " " + ctx.getOption(force)))
                                     .child(key -> new TestCommand("set") {{
                                         execute(ctx -> ctx.sendMessage("set " + key.get(ctx)));
                                     }});
        }});

        assertThat(runner.execute("config a -f")
                         .messages()).containsExactly("a true");
        assertThat(runner.execute("config -f a set")
                         .messages()).containsExactly("set a");
        // The child commands follow the argument itself, not the options after it.
        assertThatThrownBy(() -> runner.execute("config a -f set")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void a_command_that_does_not_opt_in_still_rejects_options_after_arguments() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("ban") {{
            option(Options.flag("silent", 's'));
            argument(new Word("target"), new Word("reason").optional("none")).execute((target, reason, ctx) -> {
            });
        }});

        assertThatThrownBy(() -> runner.execute("ban Steve spam -s")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void an_option_of_an_argument_follows_the_arguments() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("give") {{
            optionsAnywhere();
            CommandOption<List<Integer>, TestCommandContext> amounts = option(Options.of("amount",
                                                                                         'a',
                                                                                         new Int("amount"))
                                                                                     .repeatable(2));
            argument(new Word("item"), new Word("target").optional("me")).execute((item,
                                                                                   target,
                                                                                   ctx) -> ctx.sendMessage(item
                                                                                           + " " + target + " "
                                                                                           + ctx.getOption(amounts)));
        }});

        assertThat(runner.execute("give apple -a 3 Steve -a 4")
                         .messages()).containsExactly("apple Steve [3, 4]");
        assertThat(runner.execute("give apple -a 3 Steve")
                         .messages()).containsExactly("apple Steve [3]");
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
