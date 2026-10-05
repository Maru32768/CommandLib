package net.kunmc.lab.commandlib;

import net.kunmc.lab.commandlib.argument.*;
import org.junit.jupiter.api.Test;

class CommonCommandHelpSnapshotTest {
    @Test
    void root_help_matches_snapshot() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(createArgumentChildHelpCommand());

        TestCommandContext ctx = runner.execute("a");

        SnapshotAssertions.assertMatchesSnapshot("command-help-root.snap", ctx.messages());
    }

    @Test
    void parent_argument_help_matches_snapshot() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(createArgumentChildHelpCommand());

        TestCommandContext ctx = runner.execute("a 123 Maru32768");

        SnapshotAssertions.assertMatchesSnapshot("command-help-parent-argument.snap", ctx.messages());
    }

    @Test
    void argument_child_command_help_matches_snapshot() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(createArgumentChildHelpCommand());

        TestCommandContext ctx = runner.execute("a 123 Maru32768 sub");

        SnapshotAssertions.assertMatchesSnapshot("command-help-argument-child-command.snap", ctx.messages());
    }

    @Test
    void argument_child_argument_help_matches_snapshot() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(createArgumentChildHelpCommand());

        TestCommandContext ctx = runner.execute("a 123 Maru32768 sub false");

        SnapshotAssertions.assertMatchesSnapshot("command-help-argument-child-argument.snap", ctx.messages());
    }

    @Test
    void argument_child_description_help_matches_snapshot() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(createArgumentChildHelpCommand());

        TestCommandContext ctx = runner.execute("a 123 Maru32768 sub2");

        SnapshotAssertions.assertMatchesSnapshot("command-help-argument-child-description.snap", ctx.messages());
    }

    @Test
    void child_command_description_help_matches_snapshot() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(createArgumentChildHelpCommand());

        TestCommandContext ctx = runner.execute("a hoge");

        SnapshotAssertions.assertMatchesSnapshot("command-help-child-description.snap", ctx.messages());
    }

    @Test
    void options_only_help_matches_snapshot() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            description("Scan command");
            option(Options.flag("force", 'f')
                          .description("Force execution"));
            option(Options.integer("limit", 'n', 10, 1, 100)
                          .description("Maximum count"));
            option(Options.string("format", 'F', "text"));
        }});

        TestCommandContext ctx = runner.execute("scan");

        SnapshotAssertions.assertMatchesSnapshot("command-help-options-only.snap", ctx.messages());
    }

    @Test
    void options_and_argument_help_matches_snapshot() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("scan") {{
            description("Scan command");
            option(Options.flag("force", 'f')
                          .description("Force execution"));
            option(Options.integer("limit", 'n', 10, 1, 100)
                          .description("Maximum count"));
            argument(new CommonStringArgument<>("target")).description("Target name");
        }});

        TestCommandContext ctx = runner.execute("scan");

        SnapshotAssertions.assertMatchesSnapshot("command-help-options-and-argument.snap", ctx.messages());
    }

    @Test
    void parent_options_with_child_help_matches_snapshot() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(createParentAndChildOptionsHelpCommand());

        TestCommandContext ctx = runner.execute("manage");

        SnapshotAssertions.assertMatchesSnapshot("command-help-parent-options-with-child.snap", ctx.messages());
    }

    @Test
    void child_options_help_matches_snapshot() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(createParentAndChildOptionsHelpCommand());

        TestCommandContext ctx = runner.execute("manage start");

        SnapshotAssertions.assertMatchesSnapshot("command-help-child-options.snap", ctx.messages());
    }

    private TestCommand createParentAndChildOptionsHelpCommand() {
        return new TestCommand("manage") {{
            description("Manage command");
            option(Options.flag("verbose", 'v')
                          .description("Verbose output"));
            addChildren(new TestCommand("start") {{
                description("Start command");
                option(Options.flag("force", 'f')
                              .description("Force start"));
            }});
        }};
    }

    private TestCommand createArgumentChildHelpCommand() {
        return new TestCommand("a") {{
            description("Root command");
            addChildren(new TestCommand("hoge") {{
                description("Hoge command");
            }});
            // These branches intentionally omit executors so each partially typed input falls through to help.
            argument(new CommonIntegerArgument<>("n"),
                     new CommonStringArgument<>("p")).description("Run root arguments")
                                                     .child((n, p) -> new TestCommand("sub") {{
                                                         description("Sub command");
                                                         argument(new CommonBooleanArgument<>("b")).description(
                                                                 "Boolean argument");
                                                         argument(new CommonFloatArgument<>("float")).child(f -> new TestCommand(
                                                                                                             "sub") {{
                                                                                                         description("Nested sub command");
                                                                                                         execute(ctx -> {
                                                                                                             ctx.sendSuccess("sub sub");
                                                                                                             ctx.sendSuccess(n.get());
                                                                                                             ctx.sendSuccess(p.get(ctx));
                                                                                                             ctx.sendSuccess(f.get());
                                                                                                         });
                                                                                                     }})
                                                                                                     .description(
                                                                                                             "Float argument");
                                                     }})
                                                     .child((n, p) -> new TestCommand("sub2") {{
                                                         description("sub2");
                                                         argument(new CommonEnumArgument<>("enum",
                                                                                           TestEnum.class)).description(
                                                                 "Enum argument");
                                                     }});
        }};
    }

    private enum TestEnum {
        ONE
    }
}
