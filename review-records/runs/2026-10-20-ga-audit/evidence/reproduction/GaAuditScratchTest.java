package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.CommonIntegerArgument;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Temporary reproduction scenarios for the 2026-10-20 GA audit. Each test records the observed behavior in its
 * assertion message; a failing assertion means the hypothesis held.
 */
class GaAuditScratchTest {
    static final class IntArg extends CommonIntegerArgument<TestCommandContext, IntArg> {
        IntArg(String name) {
            super(name);
        }
    }

    private static TestCommandSource player(String name) {
        return new TestCommandSource(name, UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)), CommandActorType.PLAYER, false, Set.of()) {
            @Override
            public boolean hasPermission(String permission) {
                return true;
            }
        };
    }

    // H1: argument names are a flat namespace across parent branch and argument child.
    @Test
    void h1_parent_and_child_argument_with_same_name() throws Exception {
        List<String> observed = new ArrayList<>();
        TestCommand command = new TestCommand("config") {{
            argument(new CommonIntegerArgument<>("key")).child(key -> new TestCommand("set") {{
                argument(new CommonStringArgument<>("key")).execute((value, ctx) -> {
                    observed.add("inputs=" + ctx.getInputs() + " arguments=" + ctx.getArguments());
                    try {
                        Integer parent = key.get();
                        observed.add("parent=" + parent + " child=" + value);
                    } catch (RuntimeException e) {
                        observed.add("parent read failed: " + e);
                    }
                });
            }});
        }};
        TestCommandRunner runner = new TestCommandRunner(command);
        runner.execute("config 5 set abc");
        assertThat(observed).as("H1 observed").containsExactly("H1-EXPECTED-TO-FAIL");
    }

    // H2: getInputs() includes literals, so getInput(i) and getArgument(i) use different indexes.
    @Test
    void h2_inputs_include_literals() throws Exception {
        List<String> observed = new ArrayList<>();
        TestCommand command = new TestCommand("a") {{
            addChildren(new TestCommand("sub") {{
                argument(new CommonStringArgument<>("x"), new IntArg("n").optional(7)).execute((x, n, ctx) -> {
                    observed.add("inputs=" + ctx.getInputs() + " arguments=" + ctx.getArguments() + " input0=" + ctx.getInput(0)
                            + " argument0=" + ctx.getArgument(0));
                });
            }});
        }};
        TestCommandRunner runner = new TestCommandRunner(command);
        runner.execute("a sub foo");
        assertThat(observed).as("H2 observed").containsExactly("H2-EXPECTED-TO-FAIL");
    }

    // H3: mutation after the tree is built: the dispatcher keeps the old tree, the renderer shows the new model.
    @Test
    void h3_mutation_after_build() throws Exception {
        TestCommand command = new TestCommand("a") {{
            execute(ctx -> ctx.sendMessage("root"));
        }};
        TestCommandRunner runner = new TestCommandRunner(command);
        command.addChildren(new TestCommand("late") {{
            execute(ctx -> ctx.sendMessage("late"));
        }});
        String rendered = CommandTreeRenderer.render(List.of(command), "test.command");
        String dispatched;
        try {
            dispatched = String.join("|", runner.execute("a late").messages());
        } catch (Exception e) {
            dispatched = e.toString();
        }
        assertThat("rendered:\n" + rendered + "\ndispatched: " + dispatched).as("H3 observed").isEqualTo("H3-EXPECTED-TO-FAIL");
    }

    // H4: the cooldown starts before the executor, so an executor that fails with ctx.fail still consumes it.
    @Test
    void h4_cooldown_consumed_by_failing_executor() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-10-20T00:00:00Z"), ZoneOffset.UTC);
        TestCommand command = new TestCommand("buy") {{
            cooldown(Cooldown.of(Duration.ofMinutes(1)).clock(clock));
            execute(ctx -> {
                throw ctx.fail("Not enough money");
            });
        }};
        TestCommandRunner runner = new TestCommandRunner(command, player("Steve"));
        List<String> first = runner.execute("buy").messages();
        List<String> second = runner.execute("buy").messages();
        assertThat("first=" + first + " second=" + second).as("H4 observed").isEqualTo("H4-EXPECTED-TO-FAIL");
    }

    // H5: adding the same child twice duplicates help lines.
    @Test
    void h5_same_child_added_twice() throws Exception {
        TestCommand child = new TestCommand("start") {{
            execute(ctx -> ctx.sendMessage("started"));
        }};
        TestCommand command = new TestCommand("game") {{
            addChildren(child);
            addChildren(child);
        }};
        TestCommandRunner runner = new TestCommandRunner(command);
        assertThat(String.join("\n", runner.execute("game").messages())).as("H5 observed").isEqualTo("H5-EXPECTED-TO-FAIL");
    }

    // H6: reading an option registered on another command returns that option's default silently.
    @Test
    void h6_option_of_another_command() throws Exception {
        List<String> observed = new ArrayList<>();
        TestCommand other = new TestCommand("other");
        CommandOption<Boolean, TestCommandContext> force = other.option(Options.flag("force", 'f'));
        TestCommand command = new TestCommand("a") {{
            execute(ctx -> {
                try {
                    observed.add("value=" + ctx.getOption(force) + " has=" + ctx.hasOption(force));
                } catch (RuntimeException e) {
                    observed.add("threw " + e);
                }
            });
        }};
        new TestCommandRunner(command).execute("a");
        assertThat(observed).as("H6 observed").containsExactly("H6-EXPECTED-TO-FAIL");
    }

    // H7: two distinct routes share one permission node (direct child vs child of an argument branch).
    @Test
    void h7_duplicate_permission_nodes() {
        TestCommand command = new TestCommand("cmd") {{
            addChildren(new TestCommand("b") {{
                permission(DefaultPermission.ALL);
                argument(new CommonStringArgument<>("c")).execute((c, ctx) -> {
                });
            }});
            argument(new CommonIntegerArgument<>("a")).child(a -> new TestCommand("b") {{
                permission(DefaultPermission.NONE);
                argument(new CommonStringArgument<>("c")).execute((c, ctx) -> {
                });
            }});
        }};
        List<String> nodes = CommandTree.permissionConfigs(List.of(command), "p")
                                        .stream()
                                        .map(x -> x.node() + "=" + x.defaultPermission())
                                        .collect(Collectors.toList());
        assertThat(nodes).as("H7 observed").containsExactly("H7-EXPECTED-TO-FAIL");
    }

    // H8: an argument instance with its own execute(...) placed mid-branch makes an intermediate executable point.
    @Test
    void h8_argument_executor_mid_branch() throws Exception {
        IntArg n = new IntArg("n");
        n.execute(ctx -> ctx.sendMessage("argument executor ran"));
        TestCommand command = new TestCommand("cmd") {{
            argument(n, new CommonStringArgument<>("s")).execute((a, b, ctx) -> ctx.sendMessage("branch executor ran"));
        }};
        TestCommandRunner runner = new TestCommandRunner(command);
        String rendered = CommandTreeRenderer.render(List.of(command), "p");
        assertThat("cmd 1 -> " + runner.execute("cmd 1").messages() + "\nrendered:\n" + rendered).as("H8 observed")
                                                                                                 .isEqualTo("H8-EXPECTED-TO-FAIL");
    }

    // H9: mutually exclusive options that are both required: accepted at build, always fails at run.
    @Test
    void h9_required_and_mutually_exclusive() throws Exception {
        TestCommand command = new TestCommand("cmd") {{
            CommandOption<Boolean, TestCommandContext> a = option(Options.flag("json", 'j').required());
            CommandOption<Boolean, TestCommandContext> b = option(Options.flag("yaml", 'y').required());
            mutuallyExclusive(a, b);
            execute(ctx -> ctx.sendMessage("ran"));
        }};
        TestCommandRunner runner = new TestCommandRunner(command);
        assertThat("cmd -j -> " + runner.execute("cmd -j").messages()).as("H9 observed").isEqualTo("H9-EXPECTED-TO-FAIL");
    }

    // H10: provide(...) after build: visibility predicate sees it (live), the runner does not (snapshot).
    @Test
    void h10_provider_added_after_build() throws Exception {
        List<String> observed = new ArrayList<>();
        TestCommand command = new TestCommand("cmd") {{
            visibleWhen(ctx -> {
                observed.add("visible sees " + ctx.find(String.class).orElse("nothing"));
                return true;
            });
            execute(ctx -> observed.add("executor sees " + ctx.find(String.class).orElse("nothing")));
        }};
        TestCommandRunner runner = new TestCommandRunner(command);
        command.provide(String.class, ctx -> "late value");
        runner.execute("cmd");
        assertThat(observed).as("H10 observed").containsExactly("H10-EXPECTED-TO-FAIL");
    }

    // H1b: same name, same type: the parent's ArgRef silently reads the child's value.
    @Test
    void h1b_parent_and_child_argument_with_same_name_and_type() throws Exception {
        List<String> observed = new ArrayList<>();
        TestCommand command = new TestCommand("config") {{
            argument(new CommonStringArgument<>("key")).child(key -> new TestCommand("set") {{
                argument(new CommonStringArgument<>("key")).execute((value, ctx) -> observed.add("parent=" + key.get() + " child=" + value
                        + " inputs=" + ctx.getInputs()));
            }});
        }};
        new TestCommandRunner(command).execute("config outer set inner");
        assertThat(observed).as("H1b observed").containsExactly("H1b-EXPECTED-TO-FAIL");
    }

    // H11: an option whose argument has the same name as a positional argument.
    @Test
    void h11_option_argument_name_equals_positional_name() throws Exception {
        List<String> observed = new ArrayList<>();
        TestCommand command = new TestCommand("cmd") {{
            CommandOption<Integer, TestCommandContext> limit = option(Options.of("limit", new IntArg("n").optional(5)));
            argument(new IntArg("n")).execute((n, ctx) -> observed.add("n=" + n + " limit=" + ctx.getOption(limit) + " input(n)=" + ctx.getInput("n")));
        }};
        TestCommandRunner runner = new TestCommandRunner(command);
        runner.execute("cmd --limit 3 9");
        assertThat(observed).as("H11 observed").containsExactly("H11-EXPECTED-TO-FAIL");
    }
}
