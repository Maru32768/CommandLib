package net.kunmc.lab.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.argument.*;
import net.kunmc.lab.commandlib.exception.ArgumentValidationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommonArgumentEdgeCaseTest {
    @Test
    void integer_argument_accepts_bounds_and_rejects_out_of_range_values() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("set") {{
            argument(new CommonIntegerArgument<>("value", 0, 10)).execute((value, ctx) -> ctx.sendMessage(value));
        }});

        assertThat(runner.execute("set 0")
                         .messages()).containsExactly("0");
        assertThat(runner.execute("set 10")
                         .messages()).containsExactly("10");
        assertThatThrownBy(() -> runner.execute("set -1")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("set 11")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void long_float_and_double_arguments_reject_out_of_range_values() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("set") {{
            addChildren(new TestCommand("long") {{
                argument(new CommonLongArgument<>("value", 0L, 5L)).execute((value, ctx) -> ctx.sendMessage(value));
            }}, new TestCommand("float") {{
                argument(new CommonFloatArgument<>("value", 0F, 1F)).execute((value, ctx) -> ctx.sendMessage(value));
            }}, new TestCommand("double") {{
                argument(new CommonDoubleArgument<>("value", -1D, 1D)).execute((value, ctx) -> ctx.sendMessage(value));
            }});
        }});

        assertThat(runner.execute("set long 5")
                         .messages()).containsExactly("5");
        assertThat(runner.execute("set float 0.5")
                         .messages()).containsExactly("0.5");
        assertThat(runner.execute("set double -1")
                         .messages()).containsExactly("-1.0");
        assertThatThrownBy(() -> runner.execute("set long 6")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("set float 1.5")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("set double -1.5")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void numeric_and_boolean_arguments_reject_malformed_input() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("set") {{
            addChildren(new TestCommand("int") {{
                argument(new CommonIntegerArgument<>("value")).execute((value, ctx) -> ctx.sendMessage(value));
            }}, new TestCommand("bool") {{
                argument(new CommonBooleanArgument<>("value")).execute((value, ctx) -> ctx.sendMessage(value));
            }});
        }});

        assertThatThrownBy(() -> runner.execute("set int abc")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("set int 1.5")).isInstanceOf(CommandSyntaxException.class);
        assertThatThrownBy(() -> runner.execute("set bool yes")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void boolean_argument_parses_false_and_suggests_both_values() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("set") {{
            argument(new CommonBooleanArgument<>("value")).execute((value, ctx) -> ctx.sendMessage(value));
        }});

        assertThat(runner.execute("set false")
                         .messages()).containsExactly("false");
        assertThat(runner.suggest("set ")).contains("true", "false");
    }

    @Test
    void quoted_string_argument_rejects_unclosed_quote_and_supports_escapes() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("say") {{
            argument(new CommonStringArgument<>("text",
                                                CommonStringArgument.Type.PHRASE_QUOTED)).execute((text, ctx) -> ctx.sendMessage(
                    text));
        }});

        assertThat(runner.execute("say \"a \\\"quoted\\\" word\"")
                         .messages()).containsExactly("a \"quoted\" word");
        assertThatThrownBy(() -> runner.execute("say \"unclosed")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void word_string_argument_rejects_quoted_phrase_tail() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("say") {{
            argument(new CommonStringArgument<>("text", CommonStringArgument.Type.WORD)).execute((text, ctx) -> {
            });
        }});

        assertThatThrownBy(() -> runner.execute("say two words")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void literal_argument_is_case_sensitive() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("mode") {{
            argument(new CommonLiteralArgument<>("value", List.of("on", "off"))).execute((value, ctx) -> {
                ctx.sendMessage("mode=" + value);
            });
        }});

        assertThat(runner.execute("mode \"on\"")
                         .messages()).containsExactly("mode=on");
        assertThat(runner.execute("mode ON")
                         .messages()).containsExactly("command.unknown.argument",
                                                      "§7mode §c§nON§rcommand.context.here");
    }

    @Test
    void supplier_based_arguments_are_evaluated_on_each_use() throws Exception {
        List<String> literals = new ArrayList<>(List.of("a"));
        Map<String, Integer> objects = new java.util.HashMap<>(Map.of("one", 1));
        List<Item> items = new ArrayList<>(List.of(new Item("sword", 1)));
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("pick") {{
            addChildren(new TestCommand("literal") {{
                argument(new CommonLiteralArgument<>("value", () -> literals)).execute((value, ctx) -> ctx.sendMessage(
                        value));
            }}, new TestCommand("object") {{
                argument(new CommonObjectArgument<>("value", () -> objects)).execute((value, ctx) -> ctx.sendMessage(
                        value));
            }}, new TestCommand("nameable") {{
                argument(new CommonNameableObjectArgument<>("value",
                                                            () -> items)).execute((value, ctx) -> ctx.sendMessage(value.id));
            }});
        }});

        literals.add("b");
        objects.put("two", 2);
        items.add(new Item("bow", 2));

        assertThat(runner.execute("pick literal b")
                         .messages()).containsExactly("b");
        assertThat(runner.execute("pick object two")
                         .messages()).containsExactly("2");
        assertThat(runner.execute("pick nameable bow")
                         .messages()).containsExactly("2");
        assertThat(runner.suggest("pick literal ")).contains("a", "b");
        assertThat(runner.suggest("pick object ")).contains("one", "two");
        assertThat(runner.suggest("pick nameable ")).contains("sword", "bow");
    }

    @Test
    void nameable_object_argument_rejects_unknown_name_and_suggests_candidates() throws Exception {
        List<Item> items = List.of(new Item("sword", 1), new Item("bow", 2));
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("equip") {{
            argument(new CommonNameableObjectArgument<>("item", items)).execute((item, ctx) -> {
                ctx.sendMessage("equipped id=" + item.id);
            });
        }});

        assertThat(runner.execute("equip axe")
                         .messages()).containsExactly("command.unknown.argument",
                                                      "§7equip §c§naxe§rcommand.context.here");
        assertThat(runner.suggest("equip ")).containsExactlyInAnyOrder("sword", "bow", "help");
        assertThat(runner.suggest("equip b")).containsExactly("bow");
    }

    @Test
    void validator_with_context_can_read_previous_argument() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("range") {{
            argument(new IntArg("min"),
                     new IntArg("max").validator((max, ctx) -> max >= (int) ctx.getArgument("min"))).execute((min, max, ctx) -> {
                ctx.sendMessage(min + ".." + max);
            });
        }});

        assertThat(runner.execute("range 1 5")
                         .messages()).containsExactly("1..5");
        assertThat(runner.execute("range 5 1")
                         .messages()).containsExactly("command.unknown.argument",
                                                      "§7range 5 §c§n1§rcommand.context.here");
    }

    @Test
    void argument_validator_can_send_custom_failure_message() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("set") {{
            argument(new IntArg("value").validator((ArgumentValidator<Integer, TestCommandContext>) (value, ctx) -> {
                if (value % 2 != 0) {
                    throw new ArgumentValidationException("value must be even", "got " + value);
                }
            })).execute((value, ctx) -> ctx.sendMessage("value=" + value));
        }});

        assertThat(runner.execute("set 2")
                         .messages()).containsExactly("value=2");
        assertThat(runner.execute("set 3")
                         .messages()).containsExactly("value must be even", "got 3");
    }

    @Test
    void validator_can_be_removed_with_null() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("set") {{
            argument(new IntArg("value").validator(x -> x > 0)
                                        .validator((java.util.function.Predicate<Integer>) null)).execute((value, ctx) -> ctx.sendMessage(
                    value));
        }});

        assertThat(runner.execute("set -1")
                         .messages()).containsExactly("-1");
    }

    @Test
    void transformer_with_context_can_use_sender() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("greet") {{
            argument(new StrArg("name").transformer((name, ctx) -> name + "@" + ctx.getActor()
                                                                                   .getName())).execute((name, ctx) -> ctx.sendMessage(
                    name));
        }});

        assertThat(runner.execute("greet Alex")
                         .messages()).containsExactly("Alex@test");
    }

    @Test
    void transformer_runs_after_validator() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("set") {{
            argument(new IntArg("value").validator(x -> x < 10)
                                        .transformer(x -> x * 100)).execute((value, ctx) -> ctx.sendMessage(value));
        }});

        assertThat(runner.execute("set 9")
                         .messages()).containsExactly("900");
        assertThat(runner.execute("set 10")
                         .messages()).hasSize(2)
                                     .first()
                                     .isEqualTo("command.unknown.argument");
    }

    @Test
    void additional_parser_recovers_from_brigadier_syntax_failure() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("set") {{
            argument(new IntArg("value").additionalParser((ctx, input) -> input.equals("max") ? Integer.MAX_VALUE : null)).execute(
                    (value, ctx) -> ctx.sendMessage(value));
        }});

        // IntegerArgumentType rejects "max" while Brigadier parses the tree, before CommandLib can recover.
        assertThatThrownBy(() -> runner.execute("set max")).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void additional_parser_returning_null_keeps_original_failure() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("give") {{
            argument(new ObjArg<>("item", Map.of("sword", 1)).additionalParser((ctx, input) -> null)).execute((item, ctx) -> {
                ctx.sendMessage("item=" + item);
            });
        }});

        assertThat(runner.execute("give axe")
                         .messages()).containsExactly("command.unknown.argument",
                                                      "§7give §c§naxe§rcommand.context.here");
    }

    @Test
    void additional_parser_result_is_validated() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("give") {{
            argument(new ObjArg<>("item", Map.of("sword", 1)).additionalParser((ctx, input) -> -1)
                                                              .validator(x -> x > 0)).execute((item, ctx) -> ctx.sendMessage(
                    "item=" + item));
        }});

        assertThat(runner.execute("give axe")
                         .messages()).first()
                                     .isEqualTo("command.unknown.argument");
    }

    @Test
    void suggestion_tooltip_is_exposed() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("pick") {{
            argument(new StrArg("value").addSuggestionAction(sb -> sb.suggest("alpha", "First letter")
                                                                     .suggest("beta", (String) null))).execute((value, ctx) -> {
            });
        }});

        assertThat(runner.suggestTooltips("pick ")).containsEntry("alpha", "First letter")
                                                   .containsEntry("beta", null);
    }

    @Test
    void suggestion_action_can_read_previous_arguments() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("pair") {{
            argument(new IntArg("first"), new StrArg("second").addSuggestionAction(sb -> {
                sb.suggest("after-" + sb.getArgument("first", Integer.class));
                sb.suggest("raw-" + sb.getInput("first"));
            })).execute((first, second, ctx) -> {
            });
        }});

        assertThat(runner.suggest("pair 7 ")).containsExactlyInAnyOrder("after-7", "raw-7");
    }

    @Test
    void suggestion_action_latest_input_is_current_token() {
        List<String> seen = new ArrayList<>();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("pick") {{
            argument(new StrArg("value").addSuggestionAction(sb -> seen.add(sb.getLatestInput()))).execute((value, ctx) -> {
            });
        }});

        runner.suggest("pick ab");

        assertThat(seen).containsExactly("ab");
    }

    @Test
    void failing_suggestion_action_propagates_exception() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("pick") {{
            argument(new StrArg("value").addSuggestionAction(sb -> {
                throw new IllegalStateException("broken suggestion");
            })).execute((value, ctx) -> {
            });
        }});

        assertThatThrownBy(() -> runner.suggest("pick ")).isInstanceOf(IllegalStateException.class)
                                                         .hasMessage("broken suggestion");
    }

    @Test
    void set_suggestion_action_null_clears_all_suggestions() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("pick") {{
            argument(new StrArg("value").addSuggestionAction(sb -> sb.suggest("alpha"))
                                        .setSuggestionAction(null)).execute((value, ctx) -> {
            });
        }});

        assertThat(runner.suggest("pick ")).containsExactly("help");
    }

    @Test
    void suggestion_actions_run_once_per_request() {
        AtomicInteger calls = new AtomicInteger();
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("pick") {{
            argument(new StrArg("value").addSuggestionAction(sb -> calls.incrementAndGet())).execute((value, ctx) -> {
            });
        }});

        runner.suggest("pick ");

        assertThat(calls).hasValue(1);
    }

    private static final class IntArg extends CommonIntegerArgument<TestCommandContext, IntArg> {
        IntArg(String name) {
            super(name);
        }
    }

    private static final class StrArg extends CommonStringArgument<TestCommandContext, StrArg> {
        StrArg(String name) {
            super(name);
        }
    }

    private static final class ObjArg<T> extends CommonObjectArgument<T, TestCommandContext, ObjArg<T>> {
        ObjArg(String name, Map<String, ? extends T> map) {
            super(name, map);
        }
    }

    private static final class Item implements Nameable {
        private final String name;
        private final int id;

        private Item(String name, int id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public String tabCompleteName() {
            return name;
        }
    }
}
