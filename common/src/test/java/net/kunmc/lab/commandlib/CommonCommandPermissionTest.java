package net.kunmc.lab.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.argument.CommonStringArgument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommonCommandPermissionTest {
    @Test
    void argument_branch_permission_is_registered_with_generated_node_by_default() {
        TestCommand command = new TestCommand("config") {{
            argument(new CommonStringArgument<>("key")).description("Config key");
        }};

        List<PermissionConfig> configs = command.permissionConfigs("test.command");

        assertThat(configs).extracting(PermissionConfig::node)
                           .containsExactly("test.command.config", "test.command.config.key");
        PermissionConfig argumentConfig = configs.get(1);
        assertThat(argumentConfig.defaultPermission()).isEqualTo(DefaultPermission.OP);
        assertThat(argumentConfig.description()).isEmpty();
    }

    @Test
    void argument_branch_permission_can_change_default_and_description() {
        TestCommand command = new TestCommand("config") {{
            argument(new CommonStringArgument<>("key")).permission(DefaultPermission.ALL, "Config key");
        }};

        List<PermissionConfig> configs = command.permissionConfigs("test.command");

        assertThat(configs).extracting(PermissionConfig::node)
                           .containsExactly("test.command.config", "test.command.config.key");
        PermissionConfig argumentConfig = configs.get(1);
        assertThat(argumentConfig.defaultPermission()).isEqualTo(DefaultPermission.ALL);
        assertThat(argumentConfig.description()).isEqualTo("Config key");
    }

    @Test
    void argument_branch_permission_can_use_custom_node() {
        TestCommand command = new TestCommand("config") {{
            argument(new CommonStringArgument<>("key")).permission("custom.config.key", DefaultPermission.NONE);
        }};

        List<PermissionConfig> configs = command.permissionConfigs("test.command");

        assertThat(configs).extracting(PermissionConfig::node)
                           .contains("custom.config.key");
        assertThat(configs.get(1)
                          .defaultPermission()).isEqualTo(DefaultPermission.NONE);
    }

    @Test
    void child_command_inherits_parent_default_permission() {
        TestCommand command = new TestCommand("game") {{
            permission(DefaultPermission.ALL);
            addChildren(new TestCommand("start") {{
            }});
        }};

        List<PermissionConfig> configs = command.permissionConfigs("test.command");

        assertThat(configs).extracting(PermissionConfig::node)
                           .containsExactly("test.command.game", "test.command.game.start");
        assertThat(configs).extracting(PermissionConfig::defaultPermission)
                           .containsExactly(DefaultPermission.ALL, DefaultPermission.ALL);
    }

    @Test
    void child_command_can_override_inherited_default_permission() {
        TestCommand command = new TestCommand("game") {{
            permission(DefaultPermission.ALL);
            addChildren(new TestCommand("start") {{
                permission(DefaultPermission.NONE);
            }});
        }};

        List<PermissionConfig> configs = command.permissionConfigs("test.command");

        assertThat(configs).extracting(PermissionConfig::defaultPermission)
                           .containsExactly(DefaultPermission.ALL, DefaultPermission.NONE);
    }

    @Test
    void argument_branch_inherits_parent_command_default_permission() {
        TestCommand command = new TestCommand("config") {{
            permission(DefaultPermission.ALL);
            argument(new CommonStringArgument<>("key"));
        }};

        List<PermissionConfig> configs = command.permissionConfigs("test.command");

        assertThat(configs).extracting(PermissionConfig::node)
                           .containsExactly("test.command.config", "test.command.config.key");
        assertThat(configs).extracting(PermissionConfig::defaultPermission)
                           .containsExactly(DefaultPermission.ALL, DefaultPermission.ALL);
    }

    @Test
    void custom_argument_branch_node_inherits_parent_default_permission() {
        TestCommand command = new TestCommand("config") {{
            permission(DefaultPermission.ALL);
            argument(new CommonStringArgument<>("key")).permission("custom.config.key");
        }};

        List<PermissionConfig> configs = command.permissionConfigs("test.command");

        assertThat(configs.get(1)
                          .node()).isEqualTo("custom.config.key");
        assertThat(configs.get(1)
                          .defaultPermission()).isEqualTo(DefaultPermission.ALL);
    }

    @Test
    void argument_child_command_inherits_argument_branch_default_permission() {
        TestCommand command = new TestCommand("config") {{
            argument(new CommonStringArgument<>("key")).permission(DefaultPermission.ALL)
                                                       .child(new TestCommand("get") {{
                                                       }});
        }};

        List<PermissionConfig> configs = command.permissionConfigs("test.command");

        assertThat(configs).extracting(PermissionConfig::node)
                           .containsExactly("test.command.config",
                                            "test.command.config.key",
                                            "test.command.config.get");
        assertThat(configs).extracting(PermissionConfig::defaultPermission)
                           .containsExactly(DefaultPermission.OP, DefaultPermission.ALL, DefaultPermission.ALL);
    }

    @Test
    void argument_child_command_permission_blocks_execution_when_missing() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            permission(DefaultPermission.ALL);
            argument(new CommonStringArgument<>("key")).permission(DefaultPermission.ALL)
                                                       .child(new TestCommand("get") {{
                                                           permission(DefaultPermission.NONE);
                                                           execute(ctx -> ctx.sendMessage("get"));
                                                       }});
        }}, new TestCommandSource("test.command.config", "test.command.config.key"));

        assertThatThrownBy(() -> runner.execute("config difficulty get")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void argument_branch_default_permission_blocks_execution_when_generated_node_is_missing() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            argument(new CommonStringArgument<>("key")).execute((key, ctx) -> ctx.sendMessage(key));
        }}, new TestCommandSource("test.command.config"));

        assertThatThrownBy(() -> runner.execute("config difficulty")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void argument_branch_default_permission_allows_execution_when_generated_node_is_granted() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            argument(new CommonStringArgument<>("key")).execute((key, ctx) -> ctx.sendMessage(key));
        }}, new TestCommandSource("test.command.config", "test.command.config.key"));

        TestCommandContext ctx = execute(runner, "config difficulty");

        assertThat(ctx.messages()).containsExactly("difficulty");
    }

    @Test
    void argument_branch_permission_blocks_execution_and_suggestions() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            argument(suggestingStringArgument()).permission("custom.config.key")
                                                .execute((key, ctx) -> ctx.sendMessage(key));
        }}, new TestCommandSource("test.command.config"));

        assertThatThrownBy(() -> runner.execute("config difficulty")).isInstanceOf(CommandSyntaxException.class);
        assertThat(runner.suggest("config ")).containsExactly("help");
    }

    @Test
    void argument_branch_permission_allows_execution_and_suggestions_when_granted() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            argument(suggestingStringArgument()).permission("custom.config.key")
                                                .execute((key, ctx) -> ctx.sendMessage(key));
        }}, new TestCommandSource("test.command.config", "custom.config.key"));

        TestCommandContext ctx = execute(runner, "config difficulty");

        assertThat(ctx.messages()).containsExactly("difficulty");
        assertThat(runner.suggest("config ")).containsExactlyInAnyOrder("difficulty", "help");
    }

    @Test
    void help_hides_argument_branch_without_permission() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            argument(new CommonStringArgument<>("key")).permission("custom.config.key")
                                                       .description("Restricted key");
        }}, new TestCommandSource("test.command.config"));

        TestCommandContext ctx = execute(runner, "config");

        assertThat(ctx.messages()).anyMatch(x -> x.contains("Usage:"));
        assertThat(ctx.messages()).noneMatch(x -> x.contains("Restricted key"));
        assertThat(ctx.messages()).noneMatch(x -> x.contains("You do not have permission"));
    }

    @Test
    void bare_command_does_not_require_argument_branch_permission() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            execute(ctx -> ctx.sendMessage("bare"));
            argument(new CommonStringArgument<>("key")).permission("custom.config.key")
                                                       .execute((key, ctx) -> ctx.sendMessage(key));
        }}, new TestCommandSource("test.command.config"));

        TestCommandContext ctx = execute(runner, "config");

        assertThat(ctx.messages()).containsExactly("bare");
    }

    @Test
    void bare_command_does_not_require_any_of_multiple_argument_branch_permissions() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            execute(ctx -> ctx.sendMessage("bare"));
            argument(new CommonStringArgument<>("key")).permission("custom.config.key")
                                                       .execute((key, ctx) -> ctx.sendMessage(key));
            argument(new CommonStringArgument<>("key"),
                     new CommonStringArgument<>("value")).permission("custom.config.key.value")
                                                         .execute((key, value, ctx) -> ctx.sendMessage(value));
        }}, new TestCommandSource("test.command.config"));

        TestCommandContext ctx = execute(runner, "config");

        assertThat(ctx.messages()).containsExactly("bare");
    }

    @Test
    void option_only_input_does_not_require_argument_branch_permission() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            execute(ctx -> ctx.sendMessage("force=" + ctx.getOption(force)));
            argument(new CommonStringArgument<>("key")).permission("custom.config.key")
                                                       .execute((key, ctx) -> ctx.sendMessage(key));
        }}, new TestCommandSource("test.command.config"));

        TestCommandContext ctx = execute(runner, "config -f");

        assertThat(ctx.messages()).containsExactly("force=true");
    }

    private TestCommandContext execute(TestCommandRunner runner, String input) {
        try {
            return runner.execute(input);
        } catch (CommandSyntaxException e) {
            throw new AssertionError(e);
        }
    }

    private CommonArgument<String, TestCommandContext, ?> suggestingStringArgument() {
        return new StrArg("key").addSuggestionAction(sb -> sb.suggest("difficulty"));
    }

    private static final class StrArg extends CommonStringArgument<TestCommandContext, StrArg> {
        StrArg(String name) {
            super(name);
        }
    }

    @Test
    void command_permission_node_can_be_overridden() {
        TestCommand command = new TestCommand("game") {{
            permission("custom.game", DefaultPermission.ALL, "Play the game");
        }};

        PermissionConfig config = command.permissionConfig("test.command");

        assertThat(config.node()).isEqualTo("custom.game");
        assertThat(config.defaultPermission()).isEqualTo(DefaultPermission.ALL);
        assertThat(config.description()).isEqualTo("Play the game");
    }

    @Test
    void grandchild_permission_node_includes_every_ancestor() {
        TestCommand command = new TestCommand("game") {{
            addChildren(new TestCommand("team") {{
                addChildren(new TestCommand("join") {{
                }});
            }});
        }};

        assertThat(command.permissionConfigs("test.command")).extracting(PermissionConfig::node)
                                                             .containsExactly("test.command.game",
                                                                              "test.command.game.team",
                                                                              "test.command.game.team.join");
    }

    @Test
    void child_command_without_permission_is_hidden_from_suggestions_and_help() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            addChildren(new TestCommand("start") {{
                description("Start the game");
                execute(ctx -> ctx.sendMessage("start"));
            }}, new TestCommand("stop") {{
                description("Stop the game");
                execute(ctx -> ctx.sendMessage("stop"));
            }});
        }}, new TestCommandSource("test.command.game", "test.command.game.start"));

        assertThat(runner.visibleChildren("game")).containsExactlyInAnyOrder("start", "help");
        assertThatThrownBy(() -> runner.execute("game stop")).isInstanceOf(CommandSyntaxException.class);

        TestCommandContext help = execute(runner, "game");
        assertThat(help.messages()).anyMatch(x -> x.contains("Start the game"))
                                   .noneMatch(x -> x.contains("Stop the game"));
    }

    @Test
    void command_without_permission_is_not_executable() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            execute(ctx -> ctx.sendMessage("game"));
        }}, new TestCommandSource());

        assertThatThrownBy(() -> runner.execute("game")).isInstanceOf(CommandSyntaxException.class);
        assertThat(runner.visibleChildren()).isEmpty();
    }

    @Test
    void alias_requires_permission_of_original_command() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            addAliases("g");
            execute(ctx -> ctx.sendMessage("game"));
        }}, new TestCommandSource());

        assertThatThrownBy(() -> runner.execute("g")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void generated_help_literal_requires_command_permission() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            addChildren(new TestCommand("start") {{
                permission(DefaultPermission.ALL);
                execute(ctx -> ctx.sendMessage("start"));
            }});
        }}, new TestCommandSource("test.command.game.start"));

        assertThatThrownBy(() -> runner.execute("game help")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void dynamic_description_is_evaluated_per_sender() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            addChildren(new TestCommand("start") {{
                description(ctx -> "Start as " + ctx.getActor()
                                                    .getName());
                execute(ctx -> ctx.sendMessage("start"));
            }});
        }});

        assertThat(execute(runner, "game").messages()).anyMatch(x -> x.contains("Start as test"));
    }

    @Test
    void argument_child_without_permission_is_hidden_from_help() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            permission(DefaultPermission.ALL);
            argument(new CommonStringArgument<>("key")).permission(DefaultPermission.ALL)
                                                       .child(new TestCommand("get") {{
                                                           description("Get value");
                                                           execute(ctx -> ctx.sendMessage("get"));
                                                       }}, new TestCommand("reset") {{
                                                           description("Reset value");
                                                           execute(ctx -> ctx.sendMessage("reset"));
                                                       }});
        }}, new TestCommandSource("test.command.config", "test.command.config.key", "test.command.config.get"));

        TestCommandContext help = execute(runner, "config");

        assertThat(help.messages()).anyMatch(x -> x.contains("Get value"))
                                   .noneMatch(x -> x.contains("Reset value"));
    }
}
