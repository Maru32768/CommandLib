package net.kunmc.lab.commandlib;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.argument.CommonIntegerArgument;
import net.kunmc.lab.commandlib.argument.CommonLiteralArgument;
import net.kunmc.lab.commandlib.argument.CommonStringArgument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandTesterTest {
    @Test
    void executesCommonCommand() throws Exception {
        CommandTester tester = new CommandTester(new TestCommand("hello") {{
            argument(new CommonStringArgument<>("name")).execute(ctx -> ctx.sendMessage("Hello " + ctx.getInput("name")));
        }});

        FakeSender sender = FakeSender.console();

        tester.execute("hello Alex", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("Hello Alex");
    }

    @Test
    void suggestsLiterals() {
        CommandTester tester = new CommandTester(new TestCommand("root") {{
            addChildren(new TestCommand("child"));
        }});

        List<String> suggestions = tester.suggestions("root ", FakeSender.console())
                                         .join()
                                         .getList()
                                         .stream()
                                         .map(com.mojang.brigadier.suggestion.Suggestion::getText)
                                         .collect(Collectors.toList());

        assertThat(suggestions).contains("child");
    }

    @Test
    void permission_failure_is_reported_as_wrapped_syntax_exception() {
        CommandTester tester = new CommandTester(new TestCommand("secure") {{
            execute(ctx -> ctx.sendMessage("ok"));
        }}, "test.command");

        assertThatThrownBy(() -> tester.execute("secure", FakeSender.unknown())).isInstanceOf(RuntimeException.class)
                                                                               .hasCauseInstanceOf(
                                                                                       CommandSyntaxException.class);
    }

    @Test
    void sender_with_granted_permission_can_execute() {
        CommandTester tester = new CommandTester(new TestCommand("secure") {{
            execute(ctx -> ctx.sendMessage("ok"));
        }}, "test.command");
        FakeSender sender = FakeSender.unknown()
                                      .permissions("test.command.secure");

        tester.execute("secure", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("ok");
    }

    @Test
    void message_colors_are_kept_in_legacy_texts() {
        CommandTester tester = new CommandTester(new TestCommand("say") {{
            execute(ctx -> {
                ctx.sendMessage("plain");
                ctx.sendSuccess("ok");
                ctx.sendWarn("warn");
                ctx.sendFailure("fail");
                ctx.sendMessageWithOption("custom", option -> option.rgb(0x123456));
            });
        }});
        FakeSender sender = FakeSender.console();

        tester.execute("say", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("plain", "ok", "warn", "fail", "custom");
        assertThat(sender.getSentMessageLegacyTexts()).containsExactly("§fplain",
                                                                       "§aok",
                                                                       "§ewarn",
                                                                       "§cfail",
                                                                       "§x§1§2§3§4§5§6custom");
    }

    @Test
    void sender_locale_is_used_as_language() {
        CommandTester tester = new CommandTester(new TestCommand("lang") {{
            execute(ctx -> ctx.sendMessage(ctx.getLanguage() + "/" + ctx.getLocale()));
        }});
        FakeSender sender = FakeSender.player("Alex")
                                      .locale("ja_jp");

        tester.execute("lang", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("ja_jp/ja_JP");
    }

    @Test
    void execute_and_get_context_returns_executed_context() {
        CommandTester tester = new CommandTester(new TestCommand("sum") {{
            argument(new CommonIntegerArgument<>("left"), new CommonIntegerArgument<>("right")).execute((left, right,
                                                                                                         ctx) -> {
            });
        }});

        TestCommandContext ctx = tester.executeAndGetContext("sum 1 2", FakeSender.console());

        assertThat(ctx.getArguments()).containsExactly(1, 2);
        assertThat(ctx.getActor()
                      .getName()).isEqualTo("Console");
    }

    @Test
    void builder_registers_multiple_commands_with_prefix() {
        CommandTester tester = CommandTester.builder()
                                            .command(new TestCommand("one") {{
                                                execute(ctx -> ctx.sendMessage("one"));
                                            }})
                                            .commands(List.of(new TestCommand("two") {{
                                                execute(ctx -> ctx.sendMessage("two"));
                                            }}))
                                            .permissionPrefix("plugin")
                                            .build();
        FakeSender sender = FakeSender.unknown()
                                      .permissions("plugin.one", "plugin.two");

        tester.execute("one", sender);
        tester.execute("two", sender);

        assertThat(sender.getSentMessageTexts()).containsExactly("one", "two");
    }

    @Test
    void builder_creates_new_command_instance_from_supplier_per_tester() {
        AtomicInteger created = new AtomicInteger();
        CommandTester.Builder builder = CommandTester.builder()
                                                     .command(() -> {
                                                         created.incrementAndGet();
                                                         return new TestCommand("one");
                                                     })
                                                     .permissionPrefix("plugin");

        builder.build();
        builder.build();

        assertThat(created).hasValue(2);
    }

    @Test
    void builder_without_commands_is_rejected() {
        assertThatThrownBy(() -> CommandTester.builder()
                                              .permissionPrefix("plugin")
                                              .build()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void builder_without_permission_prefix_is_rejected() {
        assertThatThrownBy(() -> CommandTester.builder()
                                              .command(new TestCommand("one"))
                                              .build()).isInstanceOf(NullPointerException.class)
                                                       .hasMessageContaining("permissionPrefix");
    }

    @Test
    void suggestions_are_returned_for_argument() {
        CommandTester tester = new CommandTester(new TestCommand("pick") {{
            argument(new CommonLiteralArgument<>("value", List.of("alpha", "beta"))).execute((value, ctx) -> {
            });
        }});

        List<String> suggestions = tester.suggestions("pick a", FakeSender.console())
                                         .join()
                                         .getList()
                                         .stream()
                                         .map(com.mojang.brigadier.suggestion.Suggestion::getText)
                                         .collect(Collectors.toList());

        assertThat(suggestions).containsExactlyInAnyOrder("alpha", "beta");
    }

    @Test
    void latest_context_includes_execution_on_another_thread() throws Exception {
        CommandTester tester = new CommandTester(new TestCommand("ping") {{
            execute(ctx -> ctx.sendMessage("pong"));
        }});
        TestCommandContext mainContext = tester.executeAndGetContext("ping", FakeSender.console());

        TestCommandContext[] otherContext = new TestCommandContext[1];
        Thread other = new Thread(() -> otherContext[0] = tester.executeAndGetContext("ping", FakeSender.console()));
        other.start();
        other.join();

        assertThat(mainContext).isNotNull();
        assertThat(otherContext[0]).isNotNull()
                                   .isNotSameAs(mainContext);
        assertThat(TestCommandContext.latest()).isSameAs(otherContext[0]);
    }
}
