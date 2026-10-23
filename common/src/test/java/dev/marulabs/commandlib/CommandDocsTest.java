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
    void caption_descriptions_show_the_default_text_in_the_docs_and_the_caption_in_help() throws Exception {
        Caption description = Caption.of("test.motd.description", "Show the message of the day");
        TestCommand command = new TestCommand("motd") {{
            captions(Captions.create()
                             .set(description, "Show today's message"));
            description(description);
            argument(new CommonIntegerArgument<>("day")).description(description);
        }};

        assertThat(CommandDocs.of("p", command)
                              .json()).contains("\"description\": \"Show the message of the day\"")
                                      .doesNotContain("\"description\": null");
        assertThat(new TestCommandRunner(command).execute("motd")
                                                 .messages()).contains("Show today's message")
                                                             .anyMatch(x -> x.endsWith(": Show today's message"));
    }

    @Test
    void markdown_escapes_table_cells_and_strips_color_codes() {
        TestCommand command = new TestCommand("pipe") {{
            argument(new CommonStringArgument<>("a")).description("§ea | b")
                                                     .execute((ctx, a) -> {
                                                     });
        }};

        assertThat(CommandDocs.of("p", command)
                              .markdown()).contains("| a \\| b |");
    }

    @Test
    void json_escapes_quotes_backslashes_and_control_characters() {
        TestCommand command = new TestCommand("quote") {{
            description("say \"a\\b\"\nnext\r\tend\u0001");
            execute(ctx -> {
            });
        }};

        assertThat(CommandDocs.of("p", command)
                              .json()).contains("\"description\": \"say \\\"a\\\\b\\\"\\nnext\\r\\tend\\u0001\"");
    }

    @Test
    void examples_are_listed_with_a_leading_slash() {
        TestCommand command = new TestCommand("arena") {{
            examples("/arena create lobby 16", "arena lobby join");
            addChildren(new TestCommand("create") {{
                execute(ctx -> {
                });
            }});
        }};
        CommandDocs docs = CommandDocs.of("p", command);

        assertThat(docs.markdown()).contains("Examples:\n\n- `/arena create lobby 16`\n- `/arena lobby join`\n")
                                   .containsOnlyOnce("Examples:");
        assertThat(docs.json()).contains("\"examples\": [\n        \"/arena create lobby 16\",\n        \"/arena lobby join\"\n      ]")
                               .contains("\"examples\": []");
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
                                                       .execute((ctx, key) -> {
                                                       })
                                                       .child(key -> new TestCommand("set") {{
                                                           description("Set a key");
                                                           argument(new IntArg("value").optional(0)).execute((ctx,
                                                                                                              value) -> {
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
                                                      .execute((ctx, target, words) -> {
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
