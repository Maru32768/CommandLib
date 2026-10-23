package dev.marulabs.commandlib;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.argument.*;
import dev.marulabs.commandlib.branch.UnaryArgumentBranch;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandStructureTest {
    @Test
    void shared_argument_instance_keeps_executor_per_branch() throws Exception {
        StrArg key = new StrArg("key");
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("set") {{
            argument(new CommonIntegerArgument<>("number"), key).execute((number, value, ctx) -> {
                ctx.sendMessage("number:" + value);
            });
            argument(new CommonBooleanArgument<>("flag"), key).execute((flag, value, ctx) -> {
                ctx.sendMessage("flag:" + value);
            });
        }});

        assertThat(runner.execute("set 5 x")
                         .messages()).containsExactly("number:x");
        assertThat(runner.execute("set true x")
                         .messages()).containsExactly("flag:x");
    }

    @Test
    void branch_leading_to_a_child_keeps_the_executor_of_a_branch_sharing_its_argument() throws Exception {
        for (boolean executorFirst : new boolean[]{true, false}) {
            TestCommandRunner runner = new TestCommandRunner(new TestCommand("x") {{
                if (executorFirst) {
                    argument(new StrArg("team")).execute((team, ctx) -> ctx.sendMessage("exec " + team));
                }
                argument(new StrArg("team")).child(team -> new TestCommand("add") {{
                    execute(ctx -> ctx.sendMessage("add " + team.get(ctx)));
                }});
                if (!executorFirst) {
                    argument(new StrArg("team")).execute((team, ctx) -> ctx.sendMessage("exec " + team));
                }
            }});

            assertThat(runner.execute("x red")
                             .messages()).containsExactly("exec red");
            assertThat(runner.execute("x red add")
                             .messages()).containsExactly("add red");
        }
    }

    // The bare input reaches no argument node, so it must not run a branch whose argument is required.
    @Test
    void bare_input_does_not_borrow_the_executor_of_a_branch_sharing_the_optional_argument() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("x") {{
            helpFormatter((ctx, help) -> ctx.sendMessage("help"));
            argument(new StrArg("a").optional("dflt"));
            argument(new StrArg("a")).execute((a, ctx) -> ctx.sendMessage("exec " + a));
        }});

        assertThat(runner.execute("x")
                         .messages()).containsExactly("help");
        assertThat(runner.execute("x q")
                         .messages()).containsExactly("exec q");
    }

    @Test
    void a_shared_argument_node_suggests_what_each_branch_suggests() throws Exception {
        for (boolean alphaFirst : new boolean[]{true, false}) {
            TestCommandRunner runner = new TestCommandRunner(new TestCommand("x") {{
                if (alphaFirst) {
                    argument(new StrArg("team").setSuggestionProvider(b -> b.suggest("alpha"))).execute((team,
                                                                                                         ctx) -> {
                    });
                }
                argument(new StrArg("team").setSuggestionProvider(b -> b.suggest("bravo"))).child(team -> new TestCommand("add") {{
                    execute(ctx -> {
                    });
                }});
                if (!alphaFirst) {
                    argument(new StrArg("team").setSuggestionProvider(b -> b.suggest("alpha"))).execute((team,
                                                                                                         ctx) -> {
                    });
                }
            }});

            assertThat(runner.suggest("x ")).contains("alpha", "bravo");
        }
    }

    @Test
    void a_shared_argument_instance_suggests_to_the_senders_of_each_branch() {
        for (String permission : new String[]{"perm.a", "perm.b"}) {
            StrArg team = new StrArg("team").setSuggestionProvider(b -> b.suggest("alpha"));
            TestCommandRunner runner = new TestCommandRunner(new TestCommand("x") {{
                argument(team).permission("perm.a")
                              .execute((t, ctx) -> {
                              });
                argument(team, new StrArg("member")).permission("perm.b")
                                                    .execute((t, m, ctx) -> {
                                                    });
            }}, new TestCommandSource("test.command.x", permission));

            assertThat(runner.suggest("x ")).as(permission)
                                            .contains("alpha");
        }
    }

    @Test
    void a_throwing_suggestion_provider_keeps_the_suggestions_of_the_other_branches() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("x") {{
            argument(new StrArg("team").setSuggestionProvider(b -> {
                throw new IllegalStateException("broken suggestion");
            })).execute((team, ctx) -> {
            });
            argument(new StrArg("team").setSuggestionProvider(b -> b.suggest("bravo")),
                     new StrArg("member")).execute((team, member, ctx) -> {
                     });
        }});

        assertThat(runner.suggest("x ")).containsExactlyInAnyOrder("bravo", "help");
    }

    @Test
    void a_shared_argument_node_lists_a_suggestion_with_a_tooltip_once() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("x") {{
            argument(new StrArg("team").setSuggestionProvider(b -> b.suggest("alpha", "tip"))).execute((team,
                                                                                                        ctx) -> {
            });
            argument(new StrArg("team").setSuggestionProvider(b -> b.suggest("alpha", "tip")),
                     new StrArg("member")).execute((team, member, ctx) -> {
                     });
        }});

        assertThat(runner.suggest("x ")).containsExactlyInAnyOrder("alpha", "help");
    }

    @Test
    void two_branches_executing_at_the_same_argument_are_rejected() {
        TestCommand command = new TestCommand("x") {{
            argument(new StrArg("team")).execute((team, ctx) -> ctx.sendMessage("a"));
            argument(new StrArg("team")).execute((team, ctx) -> ctx.sendMessage("b"));
        }};

        assertThatThrownBy(() -> new TestCommandRunner(command)).isInstanceOf(IllegalStateException.class)
                                                                .hasMessageContaining("more than one argument branch");
    }

    @Test
    void quotable_and_greedy_arguments_sharing_a_node_are_rejected() {
        assertThatThrownBy(() -> new TestCommand("say") {{
            argument(new CommonStringArgument<>("text"),
                     new CommonStringArgument<>("to")).execute((text, to, ctx) -> {
                     });
            argument(new CommonStringArgument<>("text", CommonStringArgument.Type.PHRASE)).execute((text, ctx) -> {
            });
        }}).isInstanceOf(IllegalArgumentException.class)
           .hasMessageContaining("parse differently: QUOTABLE_PHRASE and GREEDY_PHRASE");
    }

    // Vanilla types such as EntityArgument keep their settings in fields and have no equals.
    @Test
    void arguments_sharing_a_node_with_types_of_other_settings_are_rejected() {
        assertThatThrownBy(() -> new TestCommand("x") {{
            argument(new MaxLengthArg("v", 2), new StrArg("more")).execute((v, more, ctx) -> {
            });
            argument(new MaxLengthArg("v", 10)).execute((v, ctx) -> {
            });
        }}).isInstanceOf(IllegalArgumentException.class)
           .hasMessageContaining("parse differently");
        // Separate instances of one setting still share the node.
        new TestCommand("x") {{
            argument(new MaxLengthArg("v", 2), new StrArg("more")).execute((v, more, ctx) -> {
            });
            argument(new MaxLengthArg("v", 2)).execute((v, ctx) -> {
            });
        }};
    }

    // Vanilla 1.21.5+ ItemPredicateArgument holds a parser record built anew per instance.
    @Test
    void arguments_sharing_a_node_with_types_holding_per_instance_records_share_it() {
        new TestCommand("x") {{
            argument(new ParserHolderArg("v"), new StrArg("more")).execute((v, more, ctx) -> {
            });
            argument(new ParserHolderArg("v")).execute((v, ctx) -> {
            });
        }};
    }

    // Like vanilla's ResourceArgument, whose registry key has no equals and whose lookup differs per instance.
    @Test
    void arguments_sharing_a_node_with_registry_keys_held_in_objects_must_name_the_same_registry() {
        new TestCommand("x") {{
            argument(new RegistryArg("v", "enchantment"), new StrArg("more")).execute((v, more, ctx) -> {
            });
            argument(new RegistryArg("v", "enchantment")).execute((v, ctx) -> {
            });
        }};

        assertThatThrownBy(() -> new TestCommand("x") {{
            argument(new RegistryArg("v", "enchantment"), new StrArg("more")).execute((v, more, ctx) -> {
            });
            argument(new RegistryArg("v", "mob_effect")).execute((v, ctx) -> {
            });
        }}).isInstanceOf(IllegalArgumentException.class)
           .hasMessageContaining("'v'");
    }

    private static final class Location {
        @SuppressWarnings("unused")
        private final String namespace = "minecraft";
        @SuppressWarnings("unused")
        private final String path;

        Location(String path) {
            this.path = path;
        }
    }

    private static final class Key {
        @SuppressWarnings("unused")
        private final Location registryName = new Location("root");
        @SuppressWarnings("unused")
        private final Location location;

        Key(String registry) {
            this.location = new Location(registry);
        }
    }

    private static final class Lookup {
        @SuppressWarnings("unused")
        private final Object registry = new Object();
    }

    private static final class RegistryType implements ArgumentType<String> {
        @SuppressWarnings("unused")
        private final Key registryKey;
        @SuppressWarnings("unused")
        private final Lookup registryLookup = new Lookup();

        RegistryType(String registry) {
            this.registryKey = new Key(registry);
        }

        @Override
        public String parse(StringReader reader) {
            return reader.readUnquotedString();
        }
    }

    private static final class RegistryArg extends CommonArgument<String, TestCommandContext, RegistryArg> {
        RegistryArg(String name, String registry) {
            super(name, new RegistryType(registry));
        }

        @Override
        public String cast(Object parsedArgument) {
            return (String) parsedArgument;
        }

        @Override
        protected String parseImpl(TestCommandContext ctx) {
            return ctx.getInput(name());
        }
    }

    // Like a record, equals compares its collaborator by identity.
    private static final class Parser {
        private final Object rules = new Object();

        @Override
        public boolean equals(Object o) {
            return o instanceof Parser && ((Parser) o).rules == rules;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(rules);
        }
    }

    private static final class ParserHolderType implements ArgumentType<String> {
        @SuppressWarnings("unused")
        private final Parser parser = new Parser();

        @Override
        public String parse(StringReader reader) {
            return reader.readUnquotedString();
        }
    }

    private static final class ParserHolderArg extends CommonArgument<String, TestCommandContext, ParserHolderArg> {
        ParserHolderArg(String name) {
            super(name, new ParserHolderType());
        }

        @Override
        public String cast(Object parsedArgument) {
            return (String) parsedArgument;
        }

        @Override
        protected String parseImpl(TestCommandContext ctx) {
            return ctx.getInput(name());
        }
    }

    private static final class MaxLengthType implements ArgumentType<String> {
        private final int max;

        MaxLengthType(int max) {
            this.max = max;
        }

        @Override
        public String parse(StringReader reader) throws CommandSyntaxException {
            String value = reader.readUnquotedString();
            if (value.length() > max) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidInt()
                                                                .create(value);
            }
            return value;
        }
    }

    private static final class MaxLengthArg extends CommonArgument<String, TestCommandContext, MaxLengthArg> {
        MaxLengthArg(String name, int max) {
            super(name, new MaxLengthType(max));
        }

        @Override
        public String cast(Object parsedArgument) {
            return (String) parsedArgument;
        }

        @Override
        protected String parseImpl(TestCommandContext ctx) {
            return ctx.getInput(name());
        }
    }

    @Test
    void argument_of_an_argument_child_cannot_repeat_a_name_of_its_route() {
        TestCommand differentTypes = new TestCommand("config") {{
            argument(new IntArg("key")).child(key -> new TestCommand("set") {{
                argument(new StrArg("key")).execute((value, ctx) -> {
                });
            }});
        }};
        TestCommand sameTypes = new TestCommand("config") {{
            argument(new StrArg("key")).child(key -> new TestCommand("set") {{
                addChildren(new TestCommand("value") {{
                    argument(new StrArg("key")).execute((value, ctx) -> {
                    });
                }});
            }});
        }};

        assertThatThrownBy(() -> new TestCommandRunner(differentTypes)).isInstanceOf(IllegalStateException.class)
                                                                       .hasMessageContaining("'key'")
                                                                       .hasMessageContaining("/config <key> set");
        assertThatThrownBy(() -> new TestCommandRunner(sameTypes)).isInstanceOf(IllegalStateException.class)
                                                                  .hasMessageContaining("'key'")
                                                                  .hasMessageContaining("/config <key> set value");
    }

    @Test
    void a_command_cannot_be_added_under_itself_or_a_descendant() throws Exception {
        TestCommand loop = new TestCommand("loop");
        TestCommand a = new TestCommand("a");
        TestCommand b = new TestCommand("b");
        a.addChildren(b);

        assertThatThrownBy(() -> loop.addChildren(loop)).isInstanceOf(IllegalArgumentException.class)
                                                        .hasMessageContaining("itself");
        assertThatThrownBy(() -> b.addChildren(a)).isInstanceOf(IllegalArgumentException.class)
                                                  .hasMessageContaining("descendant");
        assertThatThrownBy(() -> b.argument(new StrArg("x"))
                                  .child(a)).isInstanceOf(IllegalArgumentException.class);
        TestCommand required = new TestCommand("r") {{
            execute(ctx -> ctx.sendMessage("ran"));
        }};
        Extractor<TestCommandContext, String> blocked = ctx -> {
            throw new CommandPrerequisiteException("blocked");
        };
        assertThatThrownBy(() -> required.require(blocked)
                                         .child(required)).isInstanceOf(IllegalArgumentException.class);

        // Nothing was attached, and the rejected child did not get the prerequisite.
        assertThat(loop.children()).isEmpty();
        assertThat(b.children()).isEmpty();
        assertThat(a.parent()).isNull();
        assertThat(new TestCommandRunner(required).execute("r")
                                                  .messages()).containsExactly("ran");
    }

    @Test
    void a_command_has_one_position_under_its_parent() {
        TestCommand show = new TestCommand("show");
        TestCommand root = new TestCommand("root") {{
            argument(new StrArg("first")).permission(DefaultPermission.ALL)
                                         .child(show);
        }};

        assertThatThrownBy(() -> root.argument(new StrArg("second"))
                                     .permission(DefaultPermission.NONE)
                                     .child(show)).isInstanceOf(IllegalArgumentException.class)
                                                  .hasMessageContaining("'show' is already a child of 'root'");
        assertThatThrownBy(() -> root.addChildren(show)).isInstanceOf(IllegalArgumentException.class);
        assertThat(show.effectiveDefaultPermission()).isEqualTo(DefaultPermission.ALL);

        TestCommand twice = new TestCommand("twice");
        TestCommand parent = new TestCommand("parent");
        assertThatThrownBy(() -> parent.addChildren(twice, twice)).isInstanceOf(IllegalArgumentException.class);
        parent.addChildren(twice);
        assertThatThrownBy(() -> parent.addChildren(twice)).isInstanceOf(IllegalArgumentException.class);
        assertThat(parent.children()).containsExactly(twice);
    }

    @Test
    void a_second_executor_is_rejected() {
        TestCommand command = new TestCommand("heal") {{
            execute(ctx -> {
            });
        }};
        UnaryArgumentBranch<String, TestCommandContext, TestCommand> branch = command.argument(new StrArg("target"))
                                                                                     .execute((target, ctx) -> {
                                                                                     });
        StrArg argument = new StrArg("x").execute(ctx -> {
        });

        assertThatThrownBy(() -> command.execute(ctx -> {
        })).isInstanceOf(IllegalStateException.class)
           .hasMessageContaining("Command 'heal' already has an executor");
        assertThatThrownBy(() -> command.require(ctx -> "player")
                                        .execute((player, ctx) -> {
                                        })).isInstanceOf(IllegalStateException.class)
                                           .hasMessageContaining("Command 'heal' already has an executor");
        assertThatThrownBy(() -> branch.executeAsync(ctx -> null)).isInstanceOf(IllegalStateException.class)
                                                                  .hasMessageContaining("branch <target> of command 'heal'");
        assertThatThrownBy(() -> branch.require(ctx -> "player")
                                       .execute((target, player, ctx) -> {
                                       })).isInstanceOf(IllegalStateException.class)
                                          .hasMessageContaining("branch <target>");
        assertThatThrownBy(() -> argument.execute(ctx -> {
        })).isInstanceOf(IllegalStateException.class)
           .hasMessageContaining("Argument 'x' already has an executor");
    }

    @Test
    void execute_rejects_null() {
        assertThatThrownBy(() -> new TestCommand("set") {{
            argument(new IntArg("value")).execute((CommandExecutor<TestCommandContext>) null);
        }}).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TestCommand("set") {{
            execute(null);
        }}).isInstanceOf(NullPointerException.class);
    }

    @Test
    void branch_of_nine_arguments_reads_each_value_typed() throws Exception {
        List<IntArg> numbers = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            numbers.add(new IntArg("n" + i));
        }
        StrArg label = new StrArg("label");
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("sum") {{
            List<CommonArgument<?, TestCommandContext, ?>> arguments = new ArrayList<>(numbers);
            arguments.add(label);
            argument(arguments).execute(ctx -> {
                int sum = 0;
                for (IntArg number : numbers) {
                    sum += ctx.getArgument(number);
                }
                String text = ctx.getArgument(label);
                ctx.sendMessage(text + ":" + sum);
            });
        }});

        assertThat(runner.execute("sum 1 2 3 4 5 6 7 8 total")
                         .messages()).containsExactly("total:36");
    }

    @Test
    void argument_list_branch_without_executor_keeps_argument_executor_set_later() throws Exception {
        StrArg value = new StrArg("value");
        TestCommand command = new TestCommand("set") {{
            argument(List.of(new IntArg("index"), value));
        }};
        value.execute(ctx -> ctx.sendMessage("argument executor"));
        TestCommandRunner runner = new TestCommandRunner(command);

        assertThat(runner.execute("set 1 x")
                         .messages()).containsExactly("argument executor");
    }

    @Test
    void empty_argument_branch_is_rejected() {
        assertThatThrownBy(() -> new TestCommand("empty") {{
            argument(List.<IntArg>of());
        }}).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void argument_branch_rejects_duplicate_argument_names() {
        assertThatThrownBy(() -> new TestCommand("dup") {{
            argument(new CommonIntegerArgument<>("value"), new CommonStringArgument<>("value"));
        }}).isInstanceOf(IllegalArgumentException.class)
           .hasMessageContaining("value");
    }

    @Test
    void empty_command_name_is_rejected() {
        assertThatThrownBy(() -> new TestCommand("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duplicate_child_command_names_are_rejected_when_added() {
        TestCommand command = new TestCommand("game");

        assertThatThrownBy(() -> command.addChildren(new TestCommand("start"),
                                                     new TestCommand("start"))).isInstanceOf(IllegalArgumentException.class)
                                                                               .hasMessageContaining("start");
        assertThat(command.children()).isEmpty();
    }

    @Test
    void child_alias_conflicting_with_sibling_name_is_rejected_when_added() {
        TestCommand command = new TestCommand("game") {{
            addChildren(new TestCommand("start"));
        }};
        TestCommand begin = new TestCommand("begin") {{
            addAliases("start");
        }};

        assertThatThrownBy(() -> command.addChildren(begin)).isInstanceOf(IllegalArgumentException.class)
                                                            .hasMessageContaining("start");
        assertThat(command.children()).extracting(CommonCommand::name)
                                      .containsExactly("start");
    }

    @Test
    void alias_added_after_child_was_added_is_checked_against_siblings() {
        TestCommand begin = new TestCommand("begin");
        new TestCommand("game") {{
            addChildren(new TestCommand("start"), begin);
        }};

        assertThatThrownBy(() -> begin.addAliases("start")).isInstanceOf(IllegalArgumentException.class)
                                                           .hasMessageContaining("start");
        assertThat(begin.aliases()).isEmpty();
    }

    @Test
    void child_name_conflicting_with_argument_name_is_rejected() {
        TestCommand command = new TestCommand("game") {{
            argument(new StrArg("target"));
        }};

        assertThatThrownBy(() -> command.addChildren(new TestCommand("target"))).isInstanceOf(IllegalArgumentException.class)
                                                                                .hasMessageContaining("target");
    }

    // Option tokens such as -f share the node of the child commands and arguments.
    @Test
    void names_that_look_like_options_are_rejected() {
        TestCommand command = new TestCommand("x");

        assertThatThrownBy(() -> command.addChildren(new TestCommand("-f"))).isInstanceOf(IllegalArgumentException.class)
                                                                            .hasMessageContaining("'-f'");
        assertThatThrownBy(() -> command.addChildren(new TestCommand("y") {{
            addAliases("--force");
        }})).hasMessageContaining("'--force'");
        assertThatThrownBy(() -> command.argument(new StrArg("-name"))).hasMessageContaining("'-name'");
        assertThatThrownBy(() -> command.argument(new StrArg(CommandOption.INTERNAL_NAME_PREFIX
                + "name"))).hasMessageContaining(CommandOption.INTERNAL_NAME_PREFIX);
        assertThatThrownBy(() -> new CommandNodeCreator<>(List.of(new TestCommand("-x")),
                                                          "test.command",
                                                          TestCommandRunner.INLINE).build()).hasMessageContaining("'-x'");
    }

    @Test
    void argument_children_of_branches_sharing_a_path_must_not_collide() {
        TestCommand command = new TestCommand("game") {{
            argument(new StrArg("target")).child(target -> new TestCommand("kick"));
        }};

        assertThatThrownBy(() -> command.argument(new StrArg("target"))
                                        .child(target -> new TestCommand("kick"))).isInstanceOf(IllegalArgumentException.class)
                                                                                  .hasMessageContaining("kick")
                                                                                  .hasMessageContaining("game <target>");
    }

    @Test
    void variable_length_branches_sharing_an_argument_prefix_are_accepted() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("sum") {{
            argument(new IntArg("a"), new IntArg("b")).execute((a, b, ctx) -> ctx.sendMessage(a + b));
            argument(new IntArg("a")).execute((a, ctx) -> ctx.sendMessage(a));
        }});

        assertThat(runner.execute("sum 1 2")
                         .messages()).containsExactly("3");
        assertThat(runner.execute("sum 1")
                         .messages()).containsExactly("1");
    }

    // The branches share the node of their first argument, so it must be usable with the permission of either branch.
    @Test
    void a_shared_argument_node_is_usable_with_the_permission_of_any_branch_that_shares_it() throws Exception {
        TestCommand give = new TestCommand("give") {{
            argument(new StrArg("player")).execute((player, ctx) -> ctx.sendMessage("one " + player));
            argument(new StrArg("player"), new IntArg("count")).execute((player,
                                                                         count,
                                                                         ctx) -> ctx.sendMessage("two " + player
                                                                                 + " " + count));
        }};
        TestCommandRunner shortBranch = new TestCommandRunner(give,
                                                              new TestCommandSource("test.command.give",
                                                                                    "test.command.give.player"));
        TestCommandRunner longBranch = new TestCommandRunner(give,
                                                             new TestCommandSource("test.command.give",
                                                                                   "test.command.give.player.count"));

        assertThat(shortBranch.visibleChildren("give")).contains("player");
        assertThat(shortBranch.execute("give Steve")
                              .messages()).containsExactly("one Steve");
        assertThatThrownBy(() -> shortBranch.execute("give Steve 3")).isInstanceOf(CommandSyntaxException.class);
        assertThat(longBranch.visibleChildren("give")).contains("player");
        assertThat(longBranch.execute("give Steve 3")
                             .messages()).containsExactly("two Steve 3");
        assertThat(longBranch.execute("give Steve")
                             .messages()).containsExactly("You do not have permission to execute this command.");
    }

    @Test
    void arguments_sharing_a_node_must_have_the_same_type() {
        assertThatThrownBy(() -> new TestCommandRunner(new TestCommand("t") {{
            argument(new IntArg("x")).execute((x, ctx) -> {
            });
            argument(new StrArg("x"), new IntArg("y")).execute((x, y, ctx) -> {
            });
        }})).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'x'");
        assertThatThrownBy(() -> new TestCommandRunner(new TestCommand("t") {{
            argument(new CommonIntegerArgument<>("x", 0, 10)).execute((x, ctx) -> {
            });
            argument(new CommonIntegerArgument<>("x", 0, 5), new IntArg("y")).execute((x, y, ctx) -> {
            });
        }})).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'x'");
    }

    // A greedy argument takes the rest of the input, so nothing after it could be reached.
    @Test
    void a_greedy_argument_must_end_its_branch() {
        assertThatThrownBy(() -> new TestCommandRunner(new TestCommand("ban") {{
            argument(new CommonStringArgument<>("reason", CommonStringArgument.Type.PHRASE),
                     new IntArg("days")).execute((reason, days, ctx) -> {
                     });
        }})).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("'reason'");
        assertThatThrownBy(() -> new TestCommandRunner(new TestCommand("ban") {{
            argument(new CommonStringArgument<>("reason", CommonStringArgument.Type.PHRASE)).execute((reason,
                                                                                                      ctx) -> {
            })
                                                                                            .child(reason -> new TestCommand("confirm"));
        }})).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("'reason'");
        assertThatThrownBy(() -> new TestCommandRunner(new TestCommand("ban") {{
            argument(new PhraseArg("reasons").variadic()).execute((reasons, ctx) -> {
            });
        }})).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("'reasons'");
    }

    @Test
    void two_branches_that_run_for_the_same_input_are_rejected() {
        assertThatThrownBy(() -> new TestCommandRunner(new TestCommand("a") {{
            argument(new StrArg("w")).execute((w, ctx) -> {
            });
            argument(new StrArg("w"), new IntArg("n").optional(7)).execute((w, n, ctx) -> {
            });
        }})).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("<w>");
        assertThatThrownBy(() -> new TestCommandRunner(new TestCommand("a") {{
            argument(new StrArg("w")).execute((w, ctx) -> {
            });
            argument(new StrArg("w")).execute((w, ctx) -> {
            });
        }})).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void a_first_argument_named_help_keeps_its_executor() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("faq") {{
            argument(new StrArg("help")).execute((topic, ctx) -> ctx.sendMessage("topic " + topic));
        }});

        assertThat(runner.execute("faq options")
                         .messages()).containsExactly("topic options");
        assertThat(runner.execute("faq help")
                         .messages()).containsExactly("topic help");
    }

    @Test
    void a_command_cannot_be_the_child_of_two_parents() {
        TestCommand start = new TestCommand("start");
        new TestCommand("game").addChildren(start);

        assertThatThrownBy(() -> new TestCommand("arena").addChildren(start)).isInstanceOf(IllegalArgumentException.class)
                                                                             .hasMessageContaining("'start'");
    }

    @Test
    void duplicate_top_level_command_names_are_rejected_at_build() {
        List<TestCommand> commands = List.of(new TestCommand("foo"), new TestCommand("bar") {{
            addAliases("foo");
        }});

        assertThatThrownBy(() -> new CommandNodeCreator<>(commands,
                                                          "test.command",
                                                          TestCommandRunner.INLINE).build()).isInstanceOf(IllegalArgumentException.class)
                                                                                            .hasMessageContaining("foo");
    }

    @Test
    void user_defined_help_child_replaces_generated_help() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            addChildren(new TestCommand("start") {{
                execute(ctx -> ctx.sendMessage("start"));
            }}, new TestCommand("help") {{
                execute(ctx -> ctx.sendMessage("custom help"));
            }});
        }});

        assertThat(runner.execute("game help")
                         .messages()).containsExactly("custom help");
    }

    @Test
    void child_aliased_help_replaces_generated_help() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            addChildren(new TestCommand("info") {{
                addAliases("help");
                execute(ctx -> ctx.sendMessage("info"));
            }});
        }});

        assertThat(runner.execute("game help")
                         .messages()).containsExactly("info");
    }

    @Test
    void generated_help_literal_prints_usage() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            addChildren(new TestCommand("start") {{
                description("Start the game");
                execute(ctx -> ctx.sendMessage("start"));
            }});
        }});

        TestCommandContext ctx = runner.execute("game help");

        assertThat(ctx.messages()).anyMatch(x -> x.contains("Usage:"));
        assertThat(ctx.messages()).anyMatch(x -> x.contains("start") && x.contains("Start the game"));
    }

    @Test
    void an_alias_cannot_repeat_the_command_name_or_another_alias() {
        TestCommand game = new TestCommand("game");
        game.addAliases("g");

        assertThatThrownBy(() -> game.addAliases("game")).isInstanceOf(IllegalArgumentException.class)
                                                         .hasMessageContaining("its own name");
        assertThatThrownBy(() -> game.addAliases("g")).isInstanceOf(IllegalArgumentException.class)
                                                      .hasMessageContaining("already one of its aliases");
        assertThatThrownBy(() -> game.addAliases("ls", "ls")).isInstanceOf(IllegalArgumentException.class)
                                                             .hasMessageContaining("already one of its aliases");
        assertThat(game.aliases()).containsExactly("g");
    }

    @Test
    void alias_shows_help_of_original_command() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("game") {{
            addAliases("g");
            addChildren(new TestCommand("start") {{
                description("Start the game");
                execute(ctx -> ctx.sendMessage("start"));
            }});
        }});

        TestCommandContext ctx = runner.execute("g");

        assertThat(ctx.messages()).anyMatch(x -> x.contains("Start the game"));
    }

    @Test
    void alias_executes_argument_branch() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("give") {{
            addAliases("g");
            argument(new CommonIntegerArgument<>("amount")).execute((amount,
                                                                     ctx) -> ctx.sendMessage("amount=" + amount));
        }});

        assertThat(runner.execute("g 3")
                         .messages()).containsExactly("amount=3");
    }

    @Test
    void three_level_nested_children_execute() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("a") {{
            addChildren(new TestCommand("b") {{
                addChildren(new TestCommand("c") {{
                    addChildren(new TestCommand("d") {{
                        execute(ctx -> ctx.sendMessage("d"));
                    }});
                }});
            }});
        }});

        assertThat(runner.execute("a b c d")
                         .messages()).containsExactly("d");
    }

    @Test
    void shorter_variable_length_branch_uses_its_own_executor() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("set") {{
            argument(new CommonStringArgument<>("name")).execute((name, ctx) -> ctx.sendMessage("one:" + name));
            argument(new CommonStringArgument<>("name"),
                     new CommonIntegerArgument<>("value")).execute((name, value, ctx) -> ctx.sendMessage("two:" + name + "=" + value));
        }});

        assertThat(runner.execute("set foo")
                         .messages()).containsExactly("one:foo");
        assertThat(runner.execute("set foo 3")
                         .messages()).containsExactly("two:foo=3");
    }

    @Test
    void every_branch_arity_passes_arguments_in_declaration_order() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("n") {{
            addChildren(new TestCommand("four") {{
                argument(i("a"),
                         i("b"),
                         i("c"),
                         i("d")).execute((a, b, c, d, ctx) -> ctx.sendMessage(List.of(a, b, c, d)));
            }}, new TestCommand("five") {{
                argument(i("a"),
                         i("b"),
                         i("c"),
                         i("d"),
                         i("e")).execute((a, b, c, d, e, ctx) -> ctx.sendMessage(List.of(a, b, c, d, e)));
            }}, new TestCommand("six") {{
                argument(i("a"),
                         i("b"),
                         i("c"),
                         i("d"),
                         i("e"),
                         i("f")).execute((a, b, c, d, e, f, ctx) -> ctx.sendMessage(List.of(a, b, c, d, e, f)));
            }}, new TestCommand("seven") {{
                argument(i("a"),
                         i("b"),
                         i("c"),
                         i("d"),
                         i("e"),
                         i("f"),
                         i("g")).execute((a, b, c, d, e, f, g, ctx) -> ctx.sendMessage(List.of(a, b, c, d, e, f, g)));
            }});
        }});

        assertThat(runner.execute("n four 1 2 3 4")
                         .messages()).containsExactly("[1, 2, 3, 4]");
        assertThat(runner.execute("n five 1 2 3 4 5")
                         .messages()).containsExactly("[1, 2, 3, 4, 5]");
        assertThat(runner.execute("n six 1 2 3 4 5 6")
                         .messages()).containsExactly("[1, 2, 3, 4, 5, 6]");
        assertThat(runner.execute("n seven 1 2 3 4 5 6 7")
                         .messages()).containsExactly("[1, 2, 3, 4, 5, 6, 7]");
    }

    @Test
    void command_uncaught_exception_handler_receives_executor_exception() throws Exception {
        List<String> handled = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("boom") {{
            addUncaughtExceptionHandler((e, ctx) -> handled.add("command:" + e.getMessage()));
            execute(ctx -> {
                throw new IllegalStateException("bare");
            });
        }});

        TestCommandContext ctx = runner.execute("boom");

        assertThat(handled).containsExactly("command:bare");
        assertThat(ctx.messages()).containsExactly("command.failed", "Check the console for details.");
    }

    @Test
    void command_and_argument_uncaught_exception_handlers_receive_argument_executor_exception() throws Exception {
        List<String> handled = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("boom") {{
            addUncaughtExceptionHandler((e, ctx) -> handled.add("command:" + e.getMessage()));
            argument(new IntArg("value").addUncaughtExceptionHandler((e,
                                                                      ctx) -> handled.add("argument:"
                                                                              + e.getMessage()))).execute((value,
                                                                                                           ctx) -> {
                                                                                  throw new IllegalStateException("value="
                                                                                          + value);
                                                                              });
        }});

        runner.execute("boom 3");

        assertThat(handled).containsExactly("command:value=3", "argument:value=3");
    }

    @Test
    void failing_uncaught_exception_handler_does_not_reach_other_handlers_twice() throws Exception {
        List<String> handled = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("boom") {{
            addUncaughtExceptionHandler((e, ctx) -> {
                handled.add("first:" + e.getMessage());
                throw new IllegalStateException("handler");
            });
            addUncaughtExceptionHandler((e, ctx) -> handled.add("second:" + e.getMessage()));
            execute(ctx -> {
                throw new IllegalStateException("bare");
            });
        }});

        TestCommandContext ctx = runner.execute("boom");

        assertThat(handled).containsExactly("first:bare", "second:bare");
        assertThat(ctx.messages()).containsExactly("command.failed", "Check the console for details.");
    }

    @Test
    void uncaught_exception_handler_receives_transformer_exception() {
        List<String> handled = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("boom") {{
            addUncaughtExceptionHandler((e, ctx) -> handled.add(e.getMessage()));
            argument(new IntArg("value").transformer(x -> {
                throw new IllegalStateException("transform");
            })).execute((value, ctx) -> ctx.sendMessage("unreachable"));
        }});

        assertThatThrownBy(() -> runner.execute("boom 1")).isInstanceOf(IllegalStateException.class)
                                                          .hasMessage("transform");
        assertThat(handled).containsExactly("transform");
    }

    @Test
    void parse_failure_returns_zero_result() throws CommandSyntaxException {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("go") {{
            argument(new CommonLiteralArgument<>("dir", List.of("north"))).execute((dir, ctx) -> {
            });
        }});

        assertThat(runner.executeAndGetResult("go north")).isEqualTo(1);
        assertThat(runner.executeAndGetResult("go south")).isZero();
    }

    @Test
    void option_parse_failure_returns_zero_result() throws CommandSyntaxException {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("go") {{
            CommandOption<Boolean, TestCommandContext> force = option(Options.flag("force", 'f'));
            option(Options.flag("verbose", 'v')
                          .requires(force));
            execute(ctx -> {
            });
        }});

        assertThat(runner.executeAndGetResult("go -v")).isZero();
    }

    private static IntArg i(String name) {
        return new IntArg(name);
    }

    private static final class IntArg extends CommonIntegerArgument<TestCommandContext, IntArg> {
        IntArg(String name) {
            super(name);
        }
    }

    private static final class PhraseArg extends CommonStringArgument<TestCommandContext, PhraseArg> {
        PhraseArg(String name) {
            super(name, Type.PHRASE);
        }
    }

    private static final class StrArg extends CommonStringArgument<TestCommandContext, StrArg> {
        StrArg(String name) {
            super(name);
        }
    }
}
