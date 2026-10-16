package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.CommonEnumArgument;
import dev.marulabs.commandlib.argument.CommonIntegerArgument;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class HelpErrorFormatterTest {
    @Test
    void custom_help_formatter_gets_the_typed_model() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("arena") {{
            description("Manage arenas");
            helpFormatter((ctx, help) -> {
                ctx.sendMessage("== " + help.path()
                                            .stream()
                                            .map(CommandHelp.Token::plainText)
                                            .collect(Collectors.joining(" "))
                        + " ==");
                ctx.sendMessage(help.description());
                help.usages()
                    .forEach(x -> ctx.sendMessage(x.plainText() + " | " + x.description() + " | " + x.tokens()
                                                                                                     .stream()
                                                                                                     .map(t -> t.kind()
                                                                                                             + (t.optional()
                                                                                                                     ? "?"
                                                                                                                     : "")
                                                                                                             + (t.variadic()
                                                                                                                     ? "*"
                                                                                                                     : ""))
                                                                                                     .collect(Collectors.joining(","))));
                help.options()
                    .forEach(x -> ctx.sendMessage("option " + x.names() + " value=" + x.hasValue()));
                help.examples()
                    .forEach(x -> ctx.sendMessage("example " + x));
            });
            examples("/arena create lobby 16");
            option(Options.flag("force", 'f'));
            addChildren(new TestCommand("create") {{
                description("Create an arena");
                argument(new CommonStringArgument<>("name"),
                         new IntArg("size").optional(16)).execute((name, size, ctx) -> {
                         });
            }}, new TestCommand("hidden") {{
                permission("hidden.node");
            }});
            argument(new CommonStringArgument<>("arena")).description("Show an arena")
                                                         .child(arena -> new TestCommand("join") {{
                                                             description("Join the arena");
                                                             execute(ctx -> {
                                                             });
                                                         }});
        }}, new TestCommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return !permission.equals("hidden.node");
            }
        });

        List<String> messages = new ArrayList<>(runner.execute("arena")
                                                      .messages());
        // The children inherit the formatter.
        messages.addAll(runner.execute("arena create")
                              .messages());

        Snapshots.assertMatches("command-help-custom-formatter.snap", String.join("\n", messages));
    }

    @Test
    void default_help_lists_the_examples() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("arena") {{
            description("Manage arenas");
            examples("arena create lobby", "/arena create lobby 32");
            addChildren(new TestCommand("create") {{
                description("Create an arena");
            }});
        }});

        TestCommandContext ctx = runner.execute("arena");

        Snapshots.assertMatches("command-help-examples.snap", String.join("\n", ctx.messages()));
    }

    @Test
    void usage_formatter_formats_the_help_lines() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("arena") {{
            usageFormatter((ctx, usage) -> usage.plainText() + (usage.description()
                                                                     .isEmpty() ? ""
                                                                             : " - " + usage.description()));
            addChildren(new TestCommand("create") {{
                description("Create an arena");
                argument(new CommonStringArgument<>("name")).description("The name");
            }});
        }});

        assertThat(runner.execute("arena")
                         .messages()).contains("/arena create - Create an arena")
                                     .doesNotContain("");
        assertThat(runner.execute("arena create")
                         .messages()).contains("/arena create <name> - The name");
    }

    @Test
    void error_formatter_gets_the_kind_of_each_failure() throws Exception {
        List<CommandError.Kind> kinds = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            errorFormatter((ctx, error) -> {
                kinds.add(error.kind());
                ctx.sendFailure("[Game]");
                error.sendDefault();
            });
            addChildren(new TestCommand("mode") {{
                argument(new CommonEnumArgument<>("mode", Mode.class)).execute((mode, ctx) -> {
                });
            }}, new TestCommand("bet") {{
                argument(new IntArg("amount").validator(x -> x > 0)).execute((amount, ctx) -> {
                });
            }}, new TestCommand("tag") {{
                option(Options.string("tag", 't', "")
                              .required());
                execute(ctx -> {
                });
            }}, new TestCommand("locked") {{
                addPrerequisite(ctx -> {
                    throw new CommandPrerequisiteException("Locked.");
                });
                execute(ctx -> {
                });
            }}, new TestCommand("broken") {{
                execute(ctx -> {
                    throw new IllegalStateException("broken");
                });
            }});
        }});

        assertThat(runner.execute("game mode foo")
                         .messages()).first()
                                     .isEqualTo("[Game]");
        runner.execute("game bet 0");
        assertThat(runner.execute("game tag")
                         .messages()).containsExactly("[Game]", "Missing required option --tag.");
        assertThat(runner.execute("game locked")
                         .messages()).containsExactly("[Game]", "Locked.");
        assertThat(runner.executeAndGetResult("game broken")).isZero();

        assertThat(kinds).containsExactly(CommandError.Kind.ARGUMENT,
                                          CommandError.Kind.VALIDATION,
                                          CommandError.Kind.OPTION,
                                          CommandError.Kind.PREREQUISITE,
                                          CommandError.Kind.UNEXPECTED);
    }

    @Test
    void help_lists_a_nested_branch_that_executes_and_has_child_commands() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            helpFormatter((ctx, help) -> help.usages()
                                             .forEach(x -> ctx.sendMessage(x.plainText())));
            argument(new CommonStringArgument<>("key")).child(key -> new TestCommand("mode") {{
                argument(new CommonStringArgument<>("value")).execute((value, ctx) -> {
                                                             })
                                                             .child(value -> new TestCommand("reset") {{
                                                                 execute(ctx -> {
                                                                 });
                                                             }});
            }});
        }});

        assertThat(runner.execute("config")
                         .messages()).containsExactlyInAnyOrder("/config <key> mode <value>",
                                                                "/config <key> mode <value> reset");
    }

    // As /execute ... run game reaches the command through a redirect, whose input starts with execute.
    @Test
    void help_and_syntax_advice_name_the_command_when_it_runs_through_a_redirect() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            addAliases("g");
            helpFormatter((ctx, help) -> help.usages()
                                             .forEach(x -> ctx.sendMessage(x.plainText())));
            errorFormatter((ctx, error) -> ctx.sendFailure(error.usage() == null ? "no usage"
                    : error.usage()
                           .plainText()));
            addChildren(new TestCommand("start") {{
                execute(ctx -> {
                });
            }});
            argument(new IntArg("rounds").validator(x -> x > 0)).execute((rounds, ctx) -> {
            });
        }});
        runner.redirectToRoot("run");

        assertThat(runner.execute("run game")
                         .messages()).containsExactly("/game start", "/game <rounds>");
        assertThat(runner.execute("run g help")
                         .messages()).containsExactly("/g start", "/g <rounds>");
        assertThat(runner.execute("run game 0")
                         .messages()).containsExactly("/game <rounds>");
    }

    @Test
    void the_help_subcommand_runs_the_prerequisites_and_reports_its_failures() throws Exception {
        List<CommandError.Kind> kinds = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            errorFormatter((ctx, error) -> {
                kinds.add(error.kind());
                error.sendDefault();
            });
            addChildren(new TestCommand("locked") {{
                addPrerequisite(ctx -> {
                    throw new CommandPrerequisiteException("Locked.");
                });
                addChildren(new TestCommand("start") {{
                    execute(ctx -> ctx.sendMessage("started"));
                }});
            }}, new TestCommand("broken") {{
                helpFormatter((ctx, help) -> {
                    throw new IllegalStateException("broken help");
                });
                addChildren(new TestCommand("start") {{
                    execute(ctx -> ctx.sendMessage("started"));
                }});
            }});
        }});

        assertThat(runner.execute("game locked help")
                         .messages()).containsExactly("Locked.");
        assertThat(runner.execute("game broken help")
                         .messages()).contains("Check the console for details.");
        assertThat(kinds).containsExactly(CommandError.Kind.PREREQUISITE, CommandError.Kind.UNEXPECTED);
    }

    @Test
    void syntax_advice_shows_the_usage_of_the_failed_branch() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            errorFormatter(ErrorFormatter.SYNTAX_ADVICE);
            usageFormatter((ctx, usage) -> usage.plainText());
            addChildren(new TestCommand("mode") {{
                option(Options.flag("force", 'f'));
                argument(new CommonEnumArgument<>("mode", Mode.class),
                         new CommonIntegerArgument<>("rounds")).execute((mode, rounds, ctx) -> {
                         });
            }}, new TestCommand("locked") {{
                addPrerequisite(ctx -> {
                    throw new CommandPrerequisiteException("Locked.");
                });
                execute(ctx -> {
                });
            }});
        }});

        assertThat(runner.execute("game mode foo 3")
                         .messages()).endsWith("Usage:", "/game mode [options] <mode> <rounds>");
        assertThat(runner.execute("game locked")
                         .messages()).containsExactly("Locked.");
    }

    @Test
    void error_formatter_that_sends_nothing_only_returns_zero() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            errorFormatter((ctx, error) -> {
            });
            addPrerequisite(ctx -> {
                throw new CommandPrerequisiteException("Locked.");
            });
            execute(ctx -> {
            });
        }});

        assertThat(runner.dispatch("game")).isZero();
    }

    private static final class IntArg extends CommonIntegerArgument<TestCommandContext, IntArg> {
        IntArg(String name) {
            super(name);
        }
    }

    private enum Mode {
        SOLO
    }
}
