package dev.marulabs.commandlib;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandOptionTest {
    @Test
    void value_option_requires_type_and_parser() {
        assertThatThrownBy(() -> new CommandOption<String, TestCommandContext>("mode",
                                                                               null,
                                                                               "",
                                                                               null,
                                                                               (ctx, name) -> ""))
                                                                                                  .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CommandOption<String, TestCommandContext>("mode",
                                                                               null,
                                                                               "",
                                                                               StringArgumentType.word(),
                                                                               null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void flag_is_available_from_context() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(force));
            });
        }});

        TestCommandContext ctx = runner.execute("scan -f Alex");

        assertThat(ctx.messages()).containsExactly("Alex:true");
    }

    @Test
    void flag_defaults_to_false() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(force));
            });
        }});

        TestCommandContext ctx = runner.execute("scan Alex");

        assertThat(ctx.messages()).containsExactly("Alex:false");
    }

    @Test
    void combined_short_flags_are_available_from_context() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Boolean, TestCommandContext> verbose = option(Options.flag("verbose", 'v'));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(force) + ":" + ctx.getOption(verbose));
            });
        }});

        TestCommandContext ctx = runner.execute("scan -fv Alex");

        assertThat(ctx.messages()).containsExactly("Alex:true:true");
    }

    @Test
    void long_flag_is_available_from_context() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(force));
            });
        }});

        TestCommandContext ctx = runner.execute("scan --force Alex");

        assertThat(ctx.messages()).containsExactly("Alex:true");
    }

    @Test
    void value_option_is_available_from_context() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<Integer, TestCommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(limit));
            });
        }});

        TestCommandContext ctx = runner.execute("scan -n 20 Alex");

        assertThat(ctx.messages()).containsExactly("Alex:20");
    }

    @Test
    void value_option_long_name_is_available_from_context() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<Integer, TestCommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(limit));
            });
        }});

        TestCommandContext ctx = runner.execute("scan --limit 20 Alex");

        assertThat(ctx.messages()).containsExactly("Alex:20");
    }

    @Test
    void value_option_defaults_when_omitted() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<Integer, TestCommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(limit));
            });
        }});

        TestCommandContext ctx = runner.execute("scan Alex");

        assertThat(ctx.messages()).containsExactly("Alex:10");
    }

    @Test
    void string_value_option_defaults_when_omitted() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<String, TestCommandContext> format = option(Options.string("format", 'F', "text"));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(format));
            });
        }});

        TestCommandContext ctx = runner.execute("scan Alex");

        assertThat(ctx.messages()).containsExactly("Alex:text");
    }

    @Test
    void string_value_option_without_value_is_rejected() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<String, TestCommandContext> format = option(Options.string("format", 'F', "text"));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(format));
            });
        }});

        assertThatThrownBy(() -> runner.execute("scan --format")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void context_reports_whether_option_is_present() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Integer, TestCommandContext> limit = option(Options.integer("limit", 'n', 10));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                ctx.sendMessage(ctx.hasOption(force) + ":" + ctx.hasOption(limit) + ":" + ctx.getOption(limit));
            });
        }});

        TestCommandContext ctx = runner.execute("scan -n 10 Alex");

        assertThat(ctx.messages()).containsExactly("false:true:10");
    }

    @Test
    void option_can_require_another_option() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<String, TestCommandContext> reason = option(Options.string("reason", 'r', "")
                                                                             .requires(force));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(force) + ":" + ctx.getOption(reason));
            });
        }});

        TestCommandContext failed = runner.execute("scan -r cleanup Alex");
        TestCommandContext passed = runner.execute("scan -f -r cleanup Alex");

        assertThat(failed.messages()).anyMatch(x -> x.contains("--reason requires --force"));
        assertThat(passed.messages()).containsExactly("Alex:true:cleanup");
    }

    @Test
    void option_can_require_another_option_value() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<String, TestCommandContext> mode = option(Options.string("mode", 'm', "normal"));
            CommandOption<Integer, TestCommandContext> limit = option(Options.integer("limit", 'n', 10)
                                                                             .requires(mode, "parallel"));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(mode) + ":" + ctx.getOption(limit));
            });
        }});

        TestCommandContext failed = runner.execute("scan -m normal -n 20 Alex");
        TestCommandContext passed = runner.execute("scan -n 20 -m parallel Alex");

        assertThat(failed.messages()).anyMatch(x -> x.contains("--limit requires --mode to be parallel"));
        assertThat(passed.messages()).containsExactly("Alex:parallel:20");
    }

    @Test
    void option_value_requirement_uses_default_value() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<String, TestCommandContext> mode = option(Options.string("mode", 'm', "parallel"));
            CommandOption<Integer, TestCommandContext> limit = option(Options.integer("limit", 'n', 10)
                                                                             .requires(mode, "parallel"));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(mode) + ":" + ctx.getOption(limit));
            });
        }});

        TestCommandContext ctx = runner.execute("scan -n 20 Alex");

        assertThat(ctx.messages()).containsExactly("Alex:parallel:20");
    }

    @Test
    void option_after_argument_is_rejected() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            argument(new CommonStringArgument<>("target", CommonStringArgument.Type.WORD)).execute((target,
                                                                                                    ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(force));
            });
        }});

        assertThatThrownBy(() -> runner.execute("scan Alex -f")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void child_command_uses_child_options_after_child_name() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            addChildren(new TestCommand("start") {{
                CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
                argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                    ctx.sendMessage(target + ":" + ctx.getOption(force));
                });
            }});
        }});

        TestCommandContext ctx = runner.execute("game start -f Alex");

        assertThat(ctx.messages()).containsExactly("Alex:true");
    }

    @Test
    void help_message_includes_options() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            option(Options.flag("force", 'f')
                          .description("Force execution"));
            option(Options.integer("limit", 'n', 10, 1, 100)
                          .description("Maximum count"));

            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
            });
        }});

        TestCommandContext ctx = runner.execute("scan");

        assertThat(ctx.messages()).anyMatch(x -> x.contains("Usage:"))
                                  .anyMatch(x -> x.contains("/scan") && x.contains("options") && x.contains("target"))
                                  .anyMatch(x -> x.contains("Options:"))
                                  .anyMatch(x -> x.contains("-f") && x.contains("--force")
                                          && x.contains("Force execution"))
                                  .anyMatch(x -> x.contains("-n") && x.contains("--limit")
                                          && x.contains("Maximum count"));
    }

    @Test
    void value_option_rejects_out_of_range_and_malformed_values() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<Integer, TestCommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(limit));
            });
        }});

        assertThatThrownBy(() -> runner.execute("scan -n 1000 Alex")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("scan -n 0 Alex")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("scan -n abc Alex")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void every_value_option_type_is_parsed() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<Boolean, TestCommandContext> bool = option(Options.bool("bool", 'b', false));
            CommandOption<Long, TestCommandContext> longValue = option(Options.longValue("long", 'l', 0L));
            CommandOption<Float, TestCommandContext> floatValue = option(Options.floatValue("float", 'x', 0F));
            CommandOption<Double, TestCommandContext> doubleValue = option(Options.doubleValue("double", 'd', 0D));
            execute(ctx -> ctx.sendMessage(ctx.getOption(bool) + ":" + ctx.getOption(longValue) + ":"
                    + ctx.getOption(floatValue) + ":" + ctx.getOption(doubleValue)));
        }});

        assertThat(runner.execute("scan -b true -l 9999999999 -x 1.5 -d 2.25")
                         .messages()).containsExactly("true:9999999999:1.5:2.25");
        assertThat(runner.execute("scan")
                         .messages()).containsExactly("false:0:0.0:0.0");
    }

    @Test
    void options_work_on_command_without_arguments() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("ping") {{
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            execute(ctx -> ctx.sendMessage("force=" + ctx.getOption(force)));
        }});

        assertThat(runner.execute("ping -f")
                         .messages()).containsExactly("force=true");
        assertThat(runner.execute("ping --force")
                         .messages()).containsExactly("force=true");
        assertThat(runner.execute("ping")
                         .messages()).containsExactly("force=false");
    }

    @Test
    void flags_can_be_combined_in_any_order_and_mixed_with_value_option() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Boolean, TestCommandContext> verbose = option(Options.flag("verbose", 'v'));
            CommandOption<Integer, TestCommandContext> limit = option(Options.integer("limit", 'n', 10));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                ctx.sendMessage(ctx.getOption(force) + ":" + ctx.getOption(verbose) + ":" + ctx.getOption(limit));
            });
        }});

        assertThat(runner.execute("scan -vf -n 5 Alex")
                         .messages()).containsExactly("true:true:5");
        assertThat(runner.execute("scan -n 5 -fv Alex")
                         .messages()).containsExactly("true:true:5");
    }

    @Test
    void same_option_cannot_be_given_twice() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            option(Options.flag("force", 'f'));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
            });
        }});

        assertThatThrownBy(() -> runner.execute("scan -f -f Alex")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("scan -f --force Alex")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void duplicate_option_names_are_rejected() {
        assertThatThrownBy(() -> new TestCommand("scan") {{
            option(Options.flag("force", 'f'));
            option(Options.flag("force", 'x'));
        }}).isInstanceOf(IllegalArgumentException.class)
           .hasMessageContaining("force");
        assertThatThrownBy(() -> new TestCommand("scan") {{
            option(Options.flag("force", 'f'));
            option(Options.flag("fast", 'f'));
        }}).isInstanceOf(IllegalArgumentException.class)
           .hasMessageContaining("short name");
    }

    @Test
    void option_requirement_with_predicate_reports_description() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<Integer, TestCommandContext> depth = option(Options.integer("depth", 'd', 1));
            CommandOption<Boolean, TestCommandContext> deep = option(Options.flag("deep", 'D')
                                                                            .requires(depth,
                                                                                      x -> x >= 3,
                                                                                      "at least 3"));
            execute(ctx -> ctx.sendMessage("deep=" + ctx.getOption(deep)));
        }});

        assertThat(runner.execute("scan -D -d 1")
                         .messages()).containsExactly("--deep requires --depth to be at least 3.");
        assertThat(runner.execute("scan -d 3 -D")
                         .messages()).containsExactly("deep=true");
    }

    @Test
    void options_can_precede_argument_child_command() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            argument(new CommonStringArgument<>("key")).child(new TestCommand("get") {{
                execute(ctx -> ctx.sendMessage("get " + ctx.getArgument("key")));
            }});
            execute(ctx -> ctx.sendMessage("force=" + ctx.getOption(force)));
        }});

        assertThat(runner.execute("config -f")
                         .messages()).containsExactly("force=true");
        assertThat(runner.execute("config -f difficulty get")
                         .messages()).containsExactly("get difficulty");
    }

    @Test
    void argument_child_does_not_count_the_options_of_its_parent() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            option(Options.flag("force", 'f'));
            argument(new CommonStringArgument<>("key")).child(new TestCommand("set") {{
                CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
                execute(ctx -> ctx.sendMessage("force=" + ctx.getOption(force)));
            }});
        }});

        assertThat(runner.execute("config -f difficulty set")
                         .messages()).containsExactly("force=false");
        assertThat(runner.execute("config difficulty set -f")
                         .messages()).containsExactly("force=true");
    }

    @Test
    void option_tokens_are_excluded_from_context_inputs() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            option(Options.flag("force", 'f'));
            option(Options.integer("limit", 'n', 10));
            argument(new CommonStringArgument<>("target")).execute((target,
                                                                    ctx) -> ctx.sendMessage(ctx.getInputs()));
        }});

        assertThat(runner.execute("scan -f -n 3 Alex")
                         .messages()).containsExactly("[scan, Alex]");
    }

    @Test
    void option_suggestions_list_long_and_short_names() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            option(Options.flag("force", 'f'));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
            });
        }});

        assertThat(runner.suggest("scan -")).contains("-f", "--force");
        assertThat(runner.suggest("scan --")).containsExactly("--force");
    }

    @Test
    void alias_works_like_the_long_name() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f')
                                                                             .aliases("yes"));
            CommandOption<Integer, TestCommandContext> limit = option(Options.integer("limit", 10)
                                                                             .aliases("max"));
            execute(ctx -> ctx.sendMessage(ctx.getOption(force) + ":" + ctx.getOption(limit)));
        }});

        assertThat(runner.execute("scan --yes --max 3")
                         .messages()).containsExactly("true:3");
        assertThat(runner.suggest("scan --")).contains("--force", "--yes", "--limit", "--max");
        // An alias is the same option, so it cannot be given after the long name.
        assertThatThrownBy(() -> runner.execute("scan --force --yes")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void alias_cannot_reuse_a_name_of_another_option() {
        assertThatThrownBy(() -> new TestCommand("scan") {{
            option(Options.flag("force"));
            option(Options.flag("yes")
                          .aliases("force"));
        }}).isInstanceOf(IllegalArgumentException.class)
           .hasMessageContaining("force");
        assertThatThrownBy(() -> Options.flag("force")
                                        .aliases("force")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void option_name_must_be_a_single_unquoted_word() {
        assertThatThrownBy(() -> Options.flag("dry run")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Options.flag("tag#2")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Options.flag("ok", ' ')).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void required_option_fails_with_option_error_when_missing() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("deploy") {{
            CommandOption<String, TestCommandContext> env = option(Options.string("env", 'e', "")
                                                                          .required());
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                ctx.sendMessage(target + ":" + ctx.getOption(env));
            });
        }});

        assertThat(runner.execute("deploy app")
                         .messages()).containsExactly("Missing required option --env.");
        assertThat(runner.execute("deploy -e prod app")
                         .messages()).containsExactly("app:prod");
        // The bare command has no executor and shows help instead of asking for the option.
        assertThat(runner.execute("deploy")
                         .messages()).anyMatch(x -> x.contains("Usage:"));
    }

    @Test
    void count_option_counts_occurrences() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            CommandOption<Integer, TestCommandContext> verbose = option(Options.count("verbose", 'v', 3));
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            execute(ctx -> ctx.sendMessage(ctx.getOption(verbose) + ":" + ctx.getOption(force) + ":"
                    + ctx.hasOption(verbose)));
        }});

        assertThat(runner.execute("scan")
                         .messages()).containsExactly("0:false:false");
        assertThat(runner.execute("scan -v")
                         .messages()).containsExactly("1:false:true");
        assertThat(runner.execute("scan -vvv")
                         .messages()).containsExactly("3:false:true");
        assertThat(runner.execute("scan -v --verbose -fv")
                         .messages()).containsExactly("3:true:true");
        assertThatThrownBy(() -> runner.execute("scan -vv -vv")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("scan -vvvv")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void repeatable_value_option_collects_values_in_input_order() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("tag") {{
            CommandOption<List<String>, TestCommandContext> tags = option(Options.string("tag", 't', "")
                                                                                 .aliases("label")
                                                                                 .description("Tag to add")
                                                                                 .repeatable(3));
            CommandOption<Integer, TestCommandContext> limit = option(Options.integer("limit", 'n', 10));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                List<String> values = ctx.getOption(tags);
                ctx.sendMessage(target + ":" + values + ":" + ctx.getOption(limit));
            });
        }});

        assertThat(runner.execute("tag Alex")
                         .messages()).containsExactly("Alex:[]:10");
        assertThat(runner.execute("tag -t red Alex")
                         .messages()).containsExactly("Alex:[red]:10");
        assertThat(runner.execute("tag -t red -n 2 --label blue --tag green Alex")
                         .messages()).containsExactly("Alex:[red, blue, green]:2");
        assertThatThrownBy(() -> runner.execute("tag -t a -t b -t c -t d Alex")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void repeatable_needs_a_value_option() {
        assertThatThrownBy(() -> Options.flag("force")
                                        .repeatable(2)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> Options.string("tag", "")
                                        .repeatable(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mutually_exclusive_options_cannot_be_given_together() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("export") {{
            CommandOption<Boolean, TestCommandContext> json = option(Options.flag("json", 'j'));
            CommandOption<Boolean, TestCommandContext> yaml = option(Options.flag("yaml", 'y'));
            CommandOption<String, TestCommandContext> format = option(Options.string("format", 'F', "text"));
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            mutuallyExclusive(json, yaml, format);
            execute(ctx -> ctx.sendMessage(ctx.getOption(json) + ":" + ctx.getOption(yaml) + ":"
                    + ctx.getOption(format) + ":" + ctx.getOption(force)));
        }});

        assertThat(runner.execute("export -jf")
                         .messages()).containsExactly("true:false:text:true");
        assertThat(runner.execute("export -f -F csv")
                         .messages()).containsExactly("false:false:csv:true");
        assertThatThrownBy(() -> runner.execute("export --json --yaml")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("export -jy")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("export -y -F csv")).isInstanceOf(CommandSyntaxException.class);
        assertThat(runner.suggest("export --json --")).containsExactly("--force");
    }

    @Test
    void mutually_exclusive_needs_registered_options() {
        assertThatThrownBy(() -> new TestCommand("export") {{
            CommandOption<Boolean, TestCommandContext> json = option(Options.flag("json"));
            mutuallyExclusive(json, Options.flag("yaml"));
        }}).isInstanceOf(IllegalArgumentException.class)
           .hasMessageContaining("--yaml");
        assertThatThrownBy(() -> new TestCommand("export") {{
            CommandOption<Boolean, TestCommandContext> json = option(Options.flag("json"));
            mutuallyExclusive(json);
        }}).isInstanceOf(IllegalArgumentException.class);
    }
}
