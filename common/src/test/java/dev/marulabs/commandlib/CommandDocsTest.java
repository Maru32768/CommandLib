package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.CommonIntegerArgument;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandDocsTest {
    @Test
    void markdown_matches_snapshot() {
        Snapshots.assertMatches("command-docs.md.snap", docs().markdown());
    }

    @Test
    void json_matches_snapshot() {
        Snapshots.assertMatches("command-docs.json.snap", docs().json());
    }

    @Test
    void permissions_markdown_matches_snapshot() {
        Snapshots.assertMatches("command-docs-permissions.md.snap", docs().permissionsMarkdown());
    }

    @Test
    void permissions_json_matches_snapshot() {
        Snapshots.assertMatches("command-docs-permissions.json.snap", docs().permissionsJson());
    }

    @Test
    void tree_matches_snapshot() {
        Snapshots.assertMatches("command-docs-tree.snap", docs().tree());
    }

    @Test
    void permissions_list_every_node_that_registration_registers() {
        List<TestCommand> commands = commands();
        String permissions = CommandDocs.of("myplugin.command", commands)
                                        .permissionsMarkdown();

        for (TestCommand command : commands) {
            for (PermissionConfig config : command.permissionConfigs("myplugin.command")) {
                assertThat(permissions).contains("`" + config.node() + "` | `" + config.defaultPermission() + "`");
            }
        }
    }

    @Test
    void sender_dependent_descriptions_are_left_out() {
        TestCommand command = new TestCommand("motd") {{
            description(ctx -> "Hello " + ctx.getActor());
            execute(ctx -> {
            });
        }};

        assertThat(CommandDocs.of("p", command)
                              .json()).contains("\"description\": null");
        assertThat(CommandDocs.of("p", command)
                              .markdown()).doesNotContain("Hello");
    }

    @Test
    void markdown_escapes_table_cells_and_strips_color_codes() {
        TestCommand command = new TestCommand("pipe") {{
            argument(new CommonStringArgument<>("a")).description("§ea | b")
                                                     .execute((a, ctx) -> {
                                                     });
        }};

        assertThat(CommandDocs.of("p", command)
                              .markdown()).contains("| a \\| b |");
    }

    @Test
    void empty_prefix_is_rejected() {
        assertThatThrownBy(() -> CommandDocs.of("", List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    private static CommandDocs docs() {
        return CommandDocs.of("myplugin.command", commands());
    }

    private static List<TestCommand> commands() {
        TestCommand config = new TestCommand("config") {{
            description("Manage the configuration");
            addAliases("cfg");
            permission(DefaultPermission.ALL, "Use /config");
            option(Options.flag("force", 'f')
                          .aliases("yes")
                          .description("Skip confirmation"));
            option(Options.string("tag", 't', "")
                          .description("Tag to add")
                          .repeatable(3)
                          .required());
            execute(ctx -> {
            });
            argument(new CommonStringArgument<>("key")).permission(DefaultPermission.OP, "Access config keys")
                                                       .description("Show a key")
                                                       .execute((key, ctx) -> {
                                                       })
                                                       .child(key -> new TestCommand("set") {{
                                                           description("Set a key");
                                                           argument(new IntArg("value").optional(0)).execute((value,
                                                                                                              ctx) -> {
                                                           });
                                                       }});
            addChildren(new TestCommand("reload") {{
                description("Reload the configuration");
                permission("myplugin.admin", DefaultPermission.NONE);
                execute(ctx -> {
                });
            }});
        }};
        TestCommand msg = new TestCommand("msg") {{
            argument(new CommonStringArgument<>("target"),
                     new WordArg("words").variadic(4)).description("Send a message")
                                                      .execute((target, words, ctx) -> {
                                                      });
        }};
        TestCommand game = new TestCommand("game") {{
            addChildren(new TestCommand("start") {{
                execute(ctx -> {
                });
            }});
        }};
        return List.of(config, msg, game);
    }

    private static final class IntArg extends CommonIntegerArgument<TestCommandContext, IntArg> {
        IntArg(String name) {
            super(name);
        }
    }

    private static final class WordArg extends CommonStringArgument<TestCommandContext, WordArg> {
        WordArg(String name) {
            super(name, Type.WORD);
        }
    }
}
