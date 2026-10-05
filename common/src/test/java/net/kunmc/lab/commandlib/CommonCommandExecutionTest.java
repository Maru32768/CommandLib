package net.kunmc.lab.commandlib;

import net.kunmc.lab.commandlib.argument.CommonIntegerArgument;
import net.kunmc.lab.commandlib.argument.CommonStringArgument;
import net.kunmc.lab.commandlib.exception.CommandPrerequisiteException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommonCommandExecutionTest {
    @Test
    void command_executes_without_arguments() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("ping") {{
            execute(ctx -> ctx.sendMessage("pong"));
        }});

        TestCommandContext ctx = runner.execute("ping");

        assertThat(ctx.messages()).containsExactly("pong");
    }

    @Test
    void alias_executes_same_command_tree() throws Exception {
        TestCommand command = new TestCommand("message") {{
            addAliases("msg");
            argument(new CommonStringArgument<>("body")).execute((body, ctx) -> {
                ctx.sendMessage(body);
            });
        }};
        TestCommandRunner runner = new TestCommandRunner(command);

        TestCommandContext ctx = runner.execute("msg hello");

        assertThat(ctx.messages()).containsExactly("hello");
    }

    @Test
    void child_command_executes_from_parent_tree() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            execute(ctx -> ctx.sendMessage("root"));
            addChildren(new TestCommand("start") {{
                execute(ctx -> ctx.sendMessage("started"));
            }});
        }});

        TestCommandContext root = runner.execute("game");
        TestCommandContext child = runner.execute("game start");

        assertThat(root.messages()).containsExactly("root");
        assertThat(child.messages()).containsExactly("started");
    }

    @Test
    void child_command_executes_with_its_own_arguments() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            addChildren(new TestCommand("start") {{
                argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                    ctx.sendMessage("started:" + target);
                });
            }});
        }});

        TestCommandContext ctx = runner.execute("game start Alex");

        assertThat(ctx.messages()).containsExactly("started:Alex");
    }

    @Test
    void argument_chain_can_have_child_command() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            argument(new CommonStringArgument<>("key")).child(key -> new TestCommand("get") {{
                execute(ctx -> {
                    ctx.sendMessage("get:" + key.get());
                });
            }});
        }});

        TestCommandContext ctx = runner.execute("config maxPlayers get");

        assertThat(ctx.messages()).containsExactly("get:maxPlayers");
    }

    @Test
    void argument_child_command_can_have_its_own_arguments() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            argument(new CommonStringArgument<>("key")).child(key -> new TestCommand("set") {{
                argument(new CommonStringArgument<>("value")).execute((value, ctx) -> {
                    ctx.sendMessage(key.get() + "=" + value);
                });
            }});
        }});

        TestCommandContext ctx = runner.execute("config difficulty set hard");

        assertThat(ctx.messages()).containsExactly("difficulty=hard");
    }

    @Test
    void argument_child_command_can_read_parent_argument_after_child_argument_is_parsed() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("counter") {{
            argument(new CommonIntegerArgument<>("count")).child(count -> new TestCommand("label") {{
                argument(new CommonStringArgument<>("name")).execute((name, ctx) -> {
                    ctx.sendMessage(count.get() + ":" + name);
                });
            }});
        }});

        TestCommandContext ctx = runner.execute("counter 3 label total");

        assertThat(ctx.messages()).containsExactly("3:total");
    }

    @Test
    void argument_branch_can_have_multiple_child_factories() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            argument(new CommonStringArgument<>("key")).child(key -> new TestCommand("get") {{
                             execute(ctx -> ctx.sendMessage("get:" + key.get()));
                         }})
                         .child(key -> new TestCommand("delete") {{
                             execute(ctx -> ctx.sendMessage("delete:" + key.get()));
                         }});
        }});

        TestCommandContext get = runner.execute("config difficulty get");
        TestCommandContext delete = runner.execute("config difficulty delete");

        assertThat(get.messages()).containsExactly("get:difficulty");
        assertThat(delete.messages()).containsExactly("delete:difficulty");
    }

    @Test
    void child_alias_executes_child_command() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            addChildren(new TestCommand("start") {{
                addAliases("run");
                execute(ctx -> ctx.sendMessage("started"));
            }});
        }});

        TestCommandContext ctx = runner.execute("game run");

        assertThat(ctx.messages()).containsExactly("started");
    }

    @Test
    void longest_argument_chain_is_selected_first() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("set") {{
            argument(new CommonStringArgument<>("key")).execute((key, ctx) -> {
                ctx.sendMessage("one:" + key);
            });
            argument(new CommonStringArgument<>("key"),
                     new CommonStringArgument<>("value")).execute((key, value, ctx) -> {
                ctx.sendMessage("two:" + key + ":" + value);
            });
        }});

        TestCommandContext ctx = runner.execute("set name Steve");

        assertThat(ctx.messages()).containsExactly("two:name:Steve");
    }

    @Test
    void prerequisite_failure_sends_message_and_blocks_execution() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("secure") {{
            addPrerequisite(ctx -> {
                throw new CommandPrerequisiteException("blocked");
            });
            execute(ctx -> ctx.sendMessage("executed"));
        }});

        TestCommandContext ctx = runner.execute("secure");

        assertThat(ctx.messages()).containsExactly("blocked");
    }

    @Test
    void child_command_inherits_parent_prerequisite_by_default() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("root") {{
            addPrerequisite(ctx -> {
                throw new CommandPrerequisiteException("parent blocked");
            });
            addChildren(new TestCommand("child") {{
                execute(ctx -> ctx.sendMessage("executed"));
            }});
        }});

        TestCommandContext ctx = runner.execute("root child");

        assertThat(ctx.messages()).containsExactly("parent blocked");
    }

    @Test
    void child_command_can_disable_parent_prerequisite() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("root") {{
            addPrerequisite(ctx -> {
                throw new CommandPrerequisiteException("parent blocked");
            });
            addChildren(new TestCommand("child") {{
                disableParentPrerequisite();
                execute(ctx -> ctx.sendMessage("executed"));
            }});
        }});

        TestCommandContext ctx = runner.execute("root child");

        assertThat(ctx.messages()).containsExactly("executed");
    }

    @Test
    void child_command_inherits_parent_preprocess_by_default() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("root") {{
            addPreprocess(ctx -> {
                ctx.sendMessage("parent preprocess");
                return true;
            });
            addChildren(new TestCommand("child") {{
                execute(ctx -> ctx.sendMessage("executed"));
            }});
        }});

        TestCommandContext ctx = runner.execute("root child");

        assertThat(ctx.messages()).containsExactly("parent preprocess", "executed");
    }

    @Test
    void child_command_can_disable_parent_preprocess() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("root") {{
            addPreprocess(ctx -> {
                ctx.sendMessage("parent preprocess");
                return true;
            });
            addChildren(new TestCommand("child") {{
                disableParentPreprocess();
                execute(ctx -> ctx.sendMessage("executed"));
            }});
        }});

        TestCommandContext ctx = runner.execute("root child");

        assertThat(ctx.messages()).containsExactly("executed");
    }

    @Test
    void preprocess_can_block_execution_after_parsing() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("run") {{
            addPreprocess(ctx -> {
                ctx.sendMessage("preprocess:" + ctx.getInput("target"));
                return false;
            });
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
                ctx.sendMessage("executed:" + target);
            });
        }});

        TestCommandContext ctx = runner.execute("run Alex");

        assertThat(ctx.messages()).containsExactly("preprocess:Alex");
    }

    @Test
    void parent_help_includes_child_commands() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            description("Game command");
            addChildren(new TestCommand("start") {{
                description("Start game");
                execute(ctx -> ctx.sendMessage("started"));
            }}, new TestCommand("stop") {{
                description("Stop game");
                execute(ctx -> ctx.sendMessage("stopped"));
            }});
        }});

        TestCommandContext ctx = runner.execute("game");

        assertThat(ctx.messages()).anyMatch(x -> x.contains("Game command"))
                                  .anyMatch(x -> x.contains("Usage:"))
                                  .anyMatch(x -> x.contains("/game"))
                                  .anyMatch(x -> x.contains("start") && x.contains("Start game"))
                                  .anyMatch(x -> x.contains("stop") && x.contains("Stop game"));
    }

    @Test
    void execution_exception_is_reported_to_sender() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("boom") {{
            execute(ctx -> {
                throw new IllegalStateException("boom");
            });
        }});

        TestCommandContext ctx = runner.execute("boom");

        assertThat(ctx.messages()).containsExactly("An unexpected error occurred trying to execute that command.",
                                                   "Check the console for details.");
    }

    @Test
    void arg_ref_resolves_parent_value_with_and_without_context() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            argument(new CommonStringArgument<>("key")).child(key -> new TestCommand("get") {{
                execute(ctx -> ctx.sendMessage(key.get() + ":" + key.get(ctx)));
            }});
        }});

        TestCommandContext ctx = runner.execute("config difficulty get");

        assertThat(ctx.messages()).containsExactly("difficulty:difficulty");
    }

    @Test
    void arg_ref_resolves_parent_value_in_child_suggestion() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            argument(new CommonStringArgument<>("key")).child(key -> new TestCommand("set") {{
                CommonArgument<String, TestCommandContext, ?> value = new CommonStringArgument<>("value");
                value.addSuggestionAction(sb -> sb.suggest(key.get() + "-value"));
                argument(value).execute((v, ctx) -> ctx.sendMessage(v));
            }});
        }});

        assertThat(runner.suggest("config difficulty set ")).contains("difficulty-value");
    }

    @Test
    void arg_ref_resolves_parent_value_in_argument_parsing_during_suggestion() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            argument(new CommonStringArgument<>("key")).child(key -> new TestCommand("set") {{
                CommonArgument<String, TestCommandContext, ?> field = new CommonStringArgument<>("field");
                field.validator((Predicate<String>) f -> key.get()
                                                          .equals("difficulty"));
                CommonArgument<String, TestCommandContext, ?> value = new CommonStringArgument<>("value");
                value.addSuggestionAction(sb -> sb.suggest(sb.getArgument("field") + "-value"));
                argument(field, value).execute((f, v, ctx) -> ctx.sendMessage(f + "=" + v));
            }});
        }});

        assertThat(runner.suggest("config difficulty set mode ")).contains("mode-value");
    }

    @Test
    void arg_ref_get_outside_command_callback_throws() throws Exception {
        AtomicReference<ArgRef<String>> captured = new AtomicReference<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            argument(new CommonStringArgument<>("key")).child(key -> {
                captured.set(key);
                return new TestCommand("get") {{
                    execute(ctx -> ctx.sendMessage(key.get()));
                }};
            });
        }});
        runner.execute("config difficulty get");

        assertThatThrownBy(() -> captured.get()
                                         .get()).isInstanceOf(IllegalStateException.class)
                                                .hasMessageContaining("key");
    }
}
