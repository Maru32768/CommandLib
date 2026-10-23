package dev.marulabs.commandlib;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.marulabs.commandlib.argument.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class CommonArgumentTest {
    @Test
    void primitive_arguments_are_parsed() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("set") {{
            argument(new CommonBooleanArgument<>("enabled"),
                     new CommonIntegerArgument<>("amount"),
                     new CommonLongArgument<>("ticks"),
                     new CommonFloatArgument<>("speed"),
                     new CommonDoubleArgument<>("ratio")).execute((ctx, enabled, amount, ticks, speed, ratio) -> {
                         ctx.sendMessage(enabled + ":" + amount + ":" + ticks + ":" + speed + ":" + ratio);
                     });
        }});

        TestCommandContext ctx = runner.execute("set true -5 9999999999 1.5 3.14");

        assertThat(ctx.messages()).containsExactly("true:-5:9999999999:1.5:3.14");
    }

    @Test
    void string_argument_parses_word_quoted_phrase_and_greedy_phrase() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("say") {{
            argument(new CommonStringArgument<>("word", CommonStringArgument.Type.WORD),
                     new CommonStringArgument<>("quoted", CommonStringArgument.Type.PHRASE_QUOTED),
                     new CommonStringArgument<>("rest", CommonStringArgument.Type.PHRASE))
                                                                                          .execute((ctx,
                                                                                                    word,
                                                                                                    quoted,
                                                                                                    rest) -> {
                                                                                              ctx.sendMessage(word
                                                                                                      + "|" + quoted
                                                                                                      + "|" + rest);
                                                                                          });
        }});

        TestCommandContext ctx = runner.execute("say hello \"good morning\" this is rest");

        assertThat(ctx.messages()).containsExactly("hello|good morning|this is rest");
    }

    @Test
    void enum_argument_resolves_case_insensitively_and_rejects_unknown_value() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("go") {{
            argument(new CommonEnumArgument<>("dir", Direction.class)).execute((ctx, dir) -> {
                ctx.sendMessage("going " + dir.name());
            });
        }});

        TestCommandContext passed = runner.execute("go north");
        TestCommandContext failed = runner.execute("go up");

        assertThat(passed.messages()).containsExactly("going NORTH");
        assertThat(failed.messages()).isNotEmpty();
        assertThat(failed.messages()).doesNotContain("going NORTH");
    }

    @Test
    void object_argument_resolves_by_name_and_rejects_unknown_value() throws Exception {
        Map<String, Integer> items = Map.of("sword", 1, "bow", 2);
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("give") {{
            argument(new CommonObjectArgument<>("item", items)).execute((ctx, item) -> {
                ctx.sendMessage("item=" + item);
            });
        }});

        TestCommandContext passed = runner.execute("give sword");
        TestCommandContext failed = runner.execute("give axe");

        assertThat(passed.messages()).containsExactly("item=1");
        assertThat(failed.messages()).isNotEmpty();
        assertThat(failed.messages()).doesNotContain("item=1");
    }

    @Test
    void nameable_object_argument_resolves_by_tab_complete_name() throws Exception {
        List<Item> items = List.of(new Item("sword", 1), new Item("bow", 2));
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("equip") {{
            argument(new CommonNameableObjectArgument<>("item", items)).execute((ctx, item) -> {
                ctx.sendMessage("equipped id=" + item.id);
            });
        }});

        TestCommandContext ctx = runner.execute("equip bow");

        assertThat(ctx.messages()).containsExactly("equipped id=2");
    }

    @Test
    void literal_argument_accepts_only_listed_values() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("mode") {{
            argument(new CommonLiteralArgument<>("value", List.of("on", "off"))).execute((ctx, value) -> {
                ctx.sendMessage("mode=" + value);
            });
        }});

        TestCommandContext passed = runner.execute("mode on");
        TestCommandContext failed = runner.execute("mode auto");

        assertThat(passed.messages()).containsExactly("mode=on");
        assertThat(failed.messages()).isNotEmpty();
        assertThat(failed.messages()).doesNotContain("mode=on");
    }

    @Test
    void validator_and_transformer_are_applied_after_parsing() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("set") {{
            argument(new IntArg("amount").validator(value -> value >= 0)
                                         .transformer(value -> value * 2)).execute((ctx, amount) -> {
                                             ctx.sendMessage("amount=" + amount);
                                         });
        }});

        TestCommandContext passed = runner.execute("set 21");
        TestCommandContext failed = runner.execute("set -1");

        assertThat(passed.messages()).containsExactly("amount=42");
        assertThat(failed.messages()).isNotEmpty();
        assertThat(failed.messages()).doesNotContain("amount=-2");
    }

    @Test
    void additional_parser_can_recover_from_library_parse_failure() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("set") {{
            argument(new ObjArg<>("item", Map.of("sword", 1)).additionalParser((ctx, input) -> input.equals("axe")
                    ? 2 : null)).execute((ctx, item) -> {
                        ctx.sendMessage("item=" + item);
                    });
        }});

        TestCommandContext ctx = runner.execute("set axe");

        assertThat(ctx.messages()).containsExactly("item=2");
    }

    @Test
    void context_exposes_raw_and_parsed_arguments() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("sum") {{
            argument(new CommonIntegerArgument<>("left"),
                     new CommonIntegerArgument<>("right")).execute((ctx, left, right) -> {
                         ctx.sendMessage(ctx.getInput("left") + ":" + ctx.getInput("right"));
                         ctx.sendMessage(ctx.getInputs()
                                            .toString());
                         ctx.sendMessage(ctx.getArguments()
                                            .toString());
                         ctx.sendMessage(ctx.getArgument("left", Integer.class) + right);
                     });
        }});

        TestCommandContext ctx = runner.execute("sum 20 22");

        assertThat(ctx.messages()).containsExactly("20:22", "[sum, 20, 22]", "[20, 22]", "42");
    }

    @Test
    void suggestions_include_custom_object_and_literal_candidates() {
        TestCommandRunner objectRunner = new TestCommandRunner(new TestCommand("give") {{
            argument(new CommonObjectArgument<>("item", Map.of("sword", 1, "bow", 2))).execute((ctx, item) -> {
            });
        }});
        TestCommandRunner literalRunner = new TestCommandRunner(new TestCommand("mode") {{
            argument(new CommonLiteralArgument<>("value", List.of("on", "off"))).execute((ctx, value) -> {
            });
        }});

        assertThat(objectRunner.suggest("give s")).containsExactly("sword");
        assertThat(literalRunner.suggest("mode o")).containsExactlyInAnyOrder("on", "off");
    }

    @Test
    void suggestions_support_async_actions() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("search") {{
            argument(new StrArg("word").addSuggestionProvider(sb -> sb.await(CompletableFuture.runAsync(() -> {
                sb.suggest("alpha");
                sb.suggest("beta");
            })))).execute((ctx, word) -> {
            });
        }});

        assertThat(runner.suggest("search ")).containsExactlyInAnyOrder("alpha", "beta", "help");
    }

    @Test
    // The suggestions future is joined, which an interrupt does not stop.
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void a_failed_async_suggestion_stage_still_completes_the_suggestions() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("search") {{
            argument(new StrArg("word").addSuggestionProvider(sb -> sb.suggest("alpha")
                                                                      .await(CompletableFuture.failedFuture(new IllegalStateException("broken"))))).execute((ctx,
                                                                                                                                                             word) -> {
                                                                      });
        }});

        assertThat(runner.suggest("search ")).containsExactlyInAnyOrder("alpha", "help");
    }

    @Test
    void argument_child_suggestions_read_parent_arguments_after_a_redirect() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("config") {{
            argument(new StrArg("key")).child(new TestCommand("set") {{
                argument(new StrArg("value").addSuggestionProvider(sb -> sb.suggest(sb.getContext()
                                                                                      .getArgument("key")
                        + "-default"))).execute((ctx, value) -> {
                        });
            }});
        }});
        // Like "execute run", which starts a new Brigadier context before the command.
        runner.redirectToRoot("run");

        assertThat(runner.suggest("config difficulty set ")).contains("difficulty-default");
        assertThat(runner.suggest("run config difficulty set ")).contains("difficulty-default");
    }

    @Test
    void default_suggestions_can_be_cleared_and_restored() {
        TestCommandRunner addedRunner = new TestCommandRunner(new TestCommand("pick") {{
            argument(new DefaultSuggestingArg("value").addSuggestionProvider(sb -> sb.suggest("custom"))).execute((ctx,
                                                                                                                   value) -> {
            });
        }});
        TestCommandRunner clearedRunner = new TestCommandRunner(new TestCommand("pick") {{
            argument(new DefaultSuggestingArg("value").clearSuggestionProviders()).execute((ctx, value) -> {
            });
        }});
        TestCommandRunner restoredRunner = new TestCommandRunner(new TestCommand("pick") {{
            argument(new DefaultSuggestingArg("value").clearSuggestionProviders()
                                                      .useDefaultSuggestions()).execute((ctx, value) -> {
                                                      });
        }});

        assertThat(addedRunner.suggest("pick ")).containsExactlyInAnyOrder("default", "custom", "help");
        assertThat(clearedRunner.suggest("pick d")).doesNotContain("default");
        assertThat(restoredRunner.suggest("pick d")).containsExactly("default");
    }

    @Test
    void default_suggestions_built_on_another_builder_are_kept() {
        // Vanilla parsers such as the item and entity selector arguments suggest through builder.createOffset(...).
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("pick") {{
            argument(new DefaultSuggestingArg("value", new OffsetSuggestingType())).execute((ctx, value) -> {
            });
        }});

        assertThat(runner.suggest("pick o")).containsExactly("offset");
    }

    private static final class IntArg extends CommonIntegerArgument<TestCommandContext, IntArg> {
        IntArg(String name) {
            super(name);
        }
    }

    private static final class ObjArg<T> extends CommonObjectArgument<T, TestCommandContext, ObjArg<T>> {
        ObjArg(String name, Map<String, ? extends T> map) {
            super(name, map);
        }
    }

    private static final class StrArg extends CommonStringArgument<TestCommandContext, StrArg> {
        StrArg(String name) {
            super(name);
        }
    }

    private static final class DefaultSuggestingArg extends CommonArgument<String, TestCommandContext, DefaultSuggestingArg> {
        DefaultSuggestingArg(String name) {
            this(name, new DefaultSuggestingType());
        }

        DefaultSuggestingArg(String name, ArgumentType<String> type) {
            super(name, type);
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

    private static final class DefaultSuggestingType implements ArgumentType<String> {
        @Override
        public String parse(StringReader reader) {
            return reader.readUnquotedString();
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context,
                                                                  SuggestionsBuilder builder) {
            builder.suggest("default");
            return builder.buildFuture();
        }
    }

    private static final class OffsetSuggestingType implements ArgumentType<String> {
        @Override
        public String parse(StringReader reader) {
            return reader.readUnquotedString();
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context,
                                                                  SuggestionsBuilder builder) {
            return builder.createOffset(builder.getStart())
                          .suggest("offset")
                          .buildFuture();
        }
    }

    enum Direction {
        NORTH,
        SOUTH,
        EAST,
        WEST
    }

    private static class Item implements Nameable {
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
