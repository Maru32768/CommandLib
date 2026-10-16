package dev.marulabs.commandlib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Options that need a permission ({@link CommandOption#permission}).
 */
class OptionPermissionTest {
    private static final String COMMAND = "test.command.ban";
    private static final String BRANCH = "test.command.ban.target";
    private static final String SILENT = "test.command.ban.option.silent";

    private static TestCommand ban() {
        return ban(false);
    }

    private static TestCommand ban(boolean anywhere) {
        return new TestCommand("ban") {{
            if (anywhere) {
                optionsAnywhere();
            }
            permission(DefaultPermission.ALL);
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Boolean, TestCommandContext> silent = option(Options.flag("silent", 's')
                                                                              .permission(DefaultPermission.OP));
            CommandOption<String, TestCommandContext> reason = option(Options.string("reason", 'r', "")
                                                                             .permission("myplugin.ban.reason"));
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> ctx.sendMessage(target
                    + " force=" + ctx.getOption(force) + " silent=" + ctx.getOption(silent) + " reason="
                    + ctx.getOption(reason)));
        }};
    }

    @Test
    void a_sender_with_the_permission_gives_the_option() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(ban(),
                                                         new TestCommandSource(COMMAND,
                                                                               BRANCH,
                                                                               SILENT,
                                                                               "myplugin.ban.reason"));

        assertThat(runner.execute("ban -fs -r spam Steve")
                         .messages()).containsExactly("Steve force=true silent=true reason=spam");
        assertThat(runner.execute("ban -f --silent Steve")
                         .messages()).containsExactly("Steve force=true silent=true reason=");
        assertThat(runner.visibleChildren("ban")).contains("-s", "--silent", "-fs", "-sf", "-r");
    }

    @Test
    void a_sender_without_it_neither_sees_nor_gives_the_option() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(ban(), new TestCommandSource(COMMAND, BRANCH));

        assertThat(runner.execute("ban -f Steve")
                         .messages()).containsExactly("Steve force=true silent=false reason=");
        assertThat(runner.visibleChildren("ban")).contains("-f", "--force")
                                                 .doesNotContain("-s", "--silent", "-fs", "-sf", "-r", "--reason");
        assertThat(runner.visibleChildren("ban", "--force")).contains("target")
                                                            .doesNotContain("-s", "--silent");
        // Brigadier tries only the literal that matches a token, so the word argument does not take it either.
        assertThatThrownBy(() -> runner.execute("ban -s")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("ban -s Steve")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("ban -f -s Steve")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("ban -fs Steve")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("ban -r spam Steve")).isInstanceOf(CommandSyntaxException.class);
    }

    // With optionsAnywhere() the argument nodes are per-state copies made by OptionGraph, a separate path that has to
    // keep the branch's requirement.
    @Test
    void a_sender_without_the_branch_permission_neither_sees_nor_runs_it_when_options_go_anywhere() throws Exception {
        TestCommandRunner allowed = new TestCommandRunner(ban(true), new TestCommandSource(COMMAND, BRANCH));
        TestCommandRunner denied = new TestCommandRunner(ban(true), new TestCommandSource(COMMAND));

        assertThat(allowed.execute("ban Steve -f")
                          .messages()).containsExactly("Steve force=true silent=false reason=");
        assertThat(allowed.visibleChildren("ban")).contains("target");
        assertThat(denied.visibleChildren("ban")).contains("-f")
                                                 .doesNotContain("target");
        assertThatThrownBy(() -> denied.execute("ban Steve")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> denied.execute("ban -f Steve")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> denied.execute("ban Steve -f")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void the_runner_rejects_an_option_whose_permission_is_gone_at_execution() throws Exception {
        assertRevokedAtExecutionIsRejected(ban(), "ban -s Steve", SILENT);
    }

    @Test
    void the_runner_rejects_a_branch_whose_permission_is_gone_at_execution() throws Exception {
        assertRevokedAtExecutionIsRejected(ban(), "ban -f Steve", BRANCH);
        assertRevokedAtExecutionIsRejected(ban(true), "ban Steve -f", BRANCH);
    }

    private static void assertRevokedAtExecutionIsRejected(TestCommand command, String input, String revoked)
            throws Exception {
        Set<String> permissions = new java.util.HashSet<>(Set.of(COMMAND, BRANCH, SILENT));
        TestCommandSource source = new TestCommandSource("test",
                                                         UUID.randomUUID(),
                                                         CommandActorType.PLAYER,
                                                         false,
                                                         Set.of()) {
            @Override
            public boolean hasPermission(String permission) {
                return permissions.contains(permission);
            }
        };
        TestCommandRunner runner = new TestCommandRunner(command, source);
        CommandDispatcher<TestCommandSource> dispatcher = new CommandDispatcher<>(runner.root());
        ParseResults<TestCommandSource> parsed = dispatcher.parse(input, source);
        permissions.remove(revoked);
        TestCommandContext.clearLatest();

        try {
            dispatcher.execute(parsed);
        } catch (CommandFailure e) {
            e.sendMessages();
        }

        assertThat(TestCommandContext.latest()
                                     .messages()).as(input)
                                                 .containsExactly("You do not have permission to execute this command.");
    }

    @Test
    void help_lists_only_the_options_the_sender_may_give() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(ban(), new TestCommandSource(COMMAND, BRANCH));

        assertThat(runner.execute("ban help")
                         .messages()).anyMatch(x -> x.contains("--force"))
                                     .noneMatch(x -> x.contains("--silent") || x.contains("--reason"));
    }

    @Test
    void the_nodes_are_registered_and_exported_with_the_command() {
        TestCommand ban = ban();

        assertThat(ban.permissionConfigs("test.command")).extracting(PermissionConfig::node)
                                                         .containsExactly(COMMAND,
                                                                          SILENT,
                                                                          "myplugin.ban.reason",
                                                                          BRANCH);
        assertThat(ban.permissionConfigs("test.command")
                      .get(1)
                      .defaultPermission()).isEqualTo(DefaultPermission.OP);
        // The exact node takes the default of the command.
        assertThat(ban.permissionConfigs("test.command")
                      .get(2)
                      .defaultPermission()).isEqualTo(DefaultPermission.ALL);

        CommandDocs docs = CommandDocs.of("test.command", List.of(ban));
        assertThat(docs.permissionsMarkdown()).contains("| `" + SILENT + "` | `OP` | `/ban --silent` |");
        assertThat(docs.permissionsJson()).contains("\"kind\": \"option\"");
        assertThat(docs.markdown()).contains("permission `" + SILENT + "` (`OP`)");
        assertThat(docs.json()).contains("\"node\": \"" + SILENT + "\"");
        assertThat(docs.tree()).contains("--silent, -s [" + SILENT + ", OP]");
    }

    @Test
    void a_repeatable_option_keeps_the_permission() {
        TestCommand command = new TestCommand("tag") {{
            option(Options.string("tag", 't', "")
                          .permission(DefaultPermission.NONE)
                          .repeatable(2));
            execute(ctx -> {
            });
        }};

        assertThat(command.permissionConfigs("p")).extracting(PermissionConfig::node)
                                                  .contains("p.tag.option.tag");
    }
}
