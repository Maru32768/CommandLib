package net.kunmc.lab.commandlib;

import com.mojang.brigadier.LiteralMessage;
import net.kunmc.lab.commandlib.argument.PlayerArgument;
import net.kunmc.lab.commandlib.argument.StringArgument;
import net.kunmc.lab.commandlib.util.nms.NMSClass;
import net.kunmc.lab.commandlib.util.nms.NMSClassRegistry;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class CommandTesterTest {
    public static class TestNMSClass extends NMSClass {
        public TestNMSClass() {
            super(null, Object.class);
        }
    }

    public static class MockTestNMSClass extends TestNMSClass {
    }

    @Nested
    class FakeSenderTest {
        @Test
        void player_is_instance_of_Player() {
            FakeSender sender = FakeSender.player("Steve");
            assertThat(sender.asSender()).isInstanceOf(Player.class);
        }

        @Test
        void console_is_instance_of_ConsoleCommandSender() {
            FakeSender sender = FakeSender.console();
            assertThat(sender.asSender()).isInstanceOf(ConsoleCommandSender.class);
        }

        @Test
        void player_name_is_set() {
            FakeSender sender = FakeSender.player("Steve");
            assertThat(((Player) sender.asSender()).getName()).isEqualTo("Steve");
        }

        @Test
        void player_locale_is_set() {
            FakeSender sender = FakeSender.player("Steve")
                                          .locale("ja_jp");
            assertThat(((Player) sender.asSender()).getLocale()).isEqualTo("ja_jp");
        }

        @Test
        void sent_messages_are_empty_before_execution() {
            FakeSender sender = FakeSender.player("Steve");
            assertThat(sender.getSentMessages()).isEmpty();
        }

        @Test
        void player_builder_sets_every_attribute() {
            UUID uuid = UUID.randomUUID();
            FakeSender sender = FakeSender.playerBuilder("Alex")
                                          .name("Steve")
                                          .uniqueId(uuid)
                                          .locale("ja_jp")
                                          .op(true)
                                          .permissions("a.b", "c.d")
                                          .denyPermissions("c.d")
                                          .build();
            Player player = (Player) sender.asSender();

            assertThat(player.getName()).isEqualTo("Steve");
            assertThat(player.getUniqueId()).isEqualTo(uuid);
            assertThat(player.getLocale()).isEqualTo("ja_jp");
            assertThat(player.isOp()).isTrue();
            assertThat(player.hasPermission("a.b")).isTrue();
            assertThat(player.hasPermission("c.d")).isFalse();
            assertThat(player.hasPermission("e.f")).isFalse();
        }

        @Test
        void console_builder_denies_listed_permissions_only() {
            FakeSender sender = FakeSender.consoleBuilder()
                                          .denyPermissions("a.b")
                                          .build();

            assertThat(sender.asSender()).isInstanceOf(ConsoleCommandSender.class);
            assertThat(sender.getName()).isEqualTo("Console");
            assertThat(sender.asSender()
                             .isOp()).isTrue();
            assertThat(sender.asSender()
                             .hasPermission("a.b")).isFalse();
            assertThat(sender.asSender()
                             .hasPermission("c.d")).isTrue();
        }
    }

    @Nested
    class Execute {
        @Test
        void message_is_captured() {
            try (CommandTester tester = new CommandTester(new Command("hello") {{
                execute(ctx -> ctx.sendMessage("Hello!"));
            }}, "test.command")) {
                FakeSender sender = FakeSender.player("Steve");
                tester.execute("hello", sender);
                assertThat(sender.getSentMessageTexts()).containsExactly("Hello!");
            }
        }

        @Test
        void send_success_has_green_color() {
            try (CommandTester tester = new CommandTester(new Command("hello") {{
                execute(ctx -> ctx.sendSuccess("done"));
            }}, "test.command")) {
                FakeSender sender = FakeSender.player("Steve");
                tester.execute("hello", sender);
                assertThat(sender.getSentMessages()).extracting(BaseComponent::getColor)
                                                    .containsExactly(ChatColor.GREEN);
            }
        }

        @Test
        void send_failure_has_red_color() {
            try (CommandTester tester = new CommandTester(new Command("hello") {{
                execute(ctx -> ctx.sendFailure("error"));
            }}, "test.command")) {
                FakeSender sender = FakeSender.player("Steve");
                tester.execute("hello", sender);
                assertThat(sender.getSentMessages()).extracting(BaseComponent::getColor)
                                                    .containsExactly(ChatColor.RED);
            }
        }

        @Test
        void multiple_messages_are_captured_in_order() {
            try (CommandTester tester = new CommandTester(new Command("hello") {{
                execute(ctx -> {
                    ctx.sendMessage("line1");
                    ctx.sendMessage("line2");
                    ctx.sendMessage("line3");
                });
            }}, "test.command")) {
                FakeSender sender = FakeSender.player("Steve");
                tester.execute("hello", sender);
                assertThat(sender.getSentMessageTexts()).containsExactly("line1", "line2", "line3");
            }
        }

        @Test
        void command_with_string_argument() {
            try (CommandTester tester = new CommandTester(new Command("greet") {{
                argument(new StringArgument("name")).execute((name, ctx) -> {
                    ctx.sendMessage("Hello, " + name + "!");
                });
            }}, "test.command")) {
                FakeSender sender = FakeSender.player("Steve");
                tester.execute("greet World", sender);
                assertThat(sender.getSentMessageTexts()).containsExactly("Hello, World!");
            }
        }

        @Test
        void command_context_returns_player_language() {
            try (CommandTester tester = new CommandTester(new Command("locale") {{
                execute(ctx -> ctx.sendMessage(ctx.getLanguage()));
            }}, "test.command")) {
                FakeSender sender = FakeSender.player("Steve")
                                              .locale("ja_jp");
                tester.execute("locale", sender);
                assertThat(sender.getSentMessageTexts()).containsExactly("ja_jp");
            }
        }

        @Test
        void command_context_returns_player_locale() {
            try (CommandTester tester = new CommandTester(new Command("locale") {{
                execute(ctx -> ctx.sendMessage(ctx.getLocale()
                                                  .toLanguageTag()));
            }}, "test.command")) {
                FakeSender sender = FakeSender.player("Steve")
                                              .locale("ja_jp");
                tester.execute("locale", sender);
                assertThat(sender.getSentMessageTexts()).containsExactly("ja-JP");
            }
        }

        @Test
        void require_player_passes_for_player_sender() {
            try (CommandTester tester = new CommandTester(new Command("playeronly") {{
                requirePlayer();
                execute(ctx -> ctx.sendMessage("ok"));
            }}, "test.command")) {
                FakeSender sender = FakeSender.player("Steve");
                tester.execute("playeronly", sender);
                assertThat(sender.getSentMessageTexts()).containsExactly("ok");
            }
        }

        @Test
        void require_player_blocks_console_sender() {
            try (CommandTester tester = new CommandTester(new Command("playeronly") {{
                requirePlayer();
                execute(ctx -> ctx.sendMessage("ok"));
            }}, "test.command")) {
                FakeSender sender = FakeSender.console();
                tester.execute("playeronly", sender);
                assertThat(sender.getSentMessageTexts()).doesNotContain("ok");
            }
        }

        @Test
        void require_console_passes_for_console_sender() {
            try (CommandTester tester = new CommandTester(new Command("consoleonly") {{
                requireConsole();
                execute(ctx -> ctx.sendMessage("ok"));
            }}, "test.command")) {
                FakeSender sender = FakeSender.console();
                tester.execute("consoleonly", sender);
                assertThat(sender.getSentMessageTexts()).containsExactly("ok");
            }
        }

        @Test
        void subcommand_message_is_captured() {
            try (CommandTester tester = new CommandTester(new Command("game") {{
                addChildren(new Command("start") {{
                    execute(ctx -> ctx.sendMessage("started"));
                }});
            }}, "test.command")) {
                FakeSender sender = FakeSender.player("Steve");
                tester.execute("game start", sender);
                assertThat(sender.getSentMessageTexts()).containsExactly("started");
            }
        }

        @Test
        void independent_testers_can_execute_on_parallel_threads() throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            try {
                Future<List<String>> first = executor.submit(parallelPlayerArgumentTask("Steve",
                                                                                        "Admin",
                                                                                        ready,
                                                                                        start));
                Future<List<String>> second = executor.submit(parallelPlayerArgumentTask("Alex",
                                                                                         "Moderator",
                                                                                         ready,
                                                                                         start));

                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
                start.countDown();

                assertThat(first.get(5, TimeUnit.SECONDS)).containsExactly("Admin -> Steve");
                assertThat(second.get(5, TimeUnit.SECONDS)).containsExactly("Moderator -> Alex");
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private Callable<List<String>> parallelPlayerArgumentTask(String targetName,
                                                              String senderName,
                                                              CountDownLatch ready,
                                                              CountDownLatch start) {
        return () -> {
            FakeSender target = FakeSender.player(targetName);
            FakeSender sender = FakeSender.player(senderName);

            try (CommandTester tester = new CommandTester(() -> new Command("who") {{
                argument(new PlayerArgument("target")).execute((targetPlayer, ctx) -> {
                    ctx.sendMessage(senderName + " -> " + targetPlayer.getName());
                });
            }}, "test.command")) {
                tester.withFakePlayer((Player) target.asSender());

                ready.countDown();
                assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();

                tester.execute("who " + targetName, sender);
                return sender.getSentMessageTexts();
            }
        };
    }

    @Nested
    class SuggestionsTest {
        @Test
        void suggestion_with_message_tooltip_exposes_tooltip_text() throws Exception {
            try (CommandTester tester = new CommandTester(new Command("cmd") {{
                argument(new StringArgument("value").addSuggestionAction(sb -> sb.suggest("alpha",
                                                                                          new LiteralMessage(
                                                                                                  "alpha tooltip"))
                                                                                 .suggest("beta"))).execute((v, ctx) -> {
                });
            }}, "test.command")) {
                FakeSender sender = FakeSender.player("Steve");

                var suggestions = tester.suggestions("cmd ", sender)
                                        .get()
                                        .getList();

                assertThat(suggestions).extracting(com.mojang.brigadier.suggestion.Suggestion::getText)
                                       .containsExactly("alpha", "beta", "help");
                assertThat(suggestions.get(0)
                                      .getTooltip()
                                      .getString()).isEqualTo("alpha tooltip");
                assertThat(suggestions.get(1)
                                      .getTooltip()).isNull();
            }
        }
    }

    @Nested
    class BuilderTest {
        @Test
        void command_can_be_registered_with_builder() {
            try (CommandTester tester = CommandTester.builder()
                                                     .command(new Command("hello") {{
                                                         execute(ctx -> ctx.sendMessage("Hello!"));
                                                     }})
                                                     .permissionPrefix("test.command")
                                                     .build()) {
                FakeSender sender = FakeSender.player("Steve");
                tester.execute("hello", sender);
                assertThat(sender.getSentMessageTexts()).containsExactly("Hello!");
            }
        }

        @Test
        void external_nms_mock_is_available_while_command_supplier_is_called() {
            try (CommandTester tester = CommandTester.builder()
                                                     .mockNmsClass(TestNMSClass.class, MockTestNMSClass.class)
                                                     .command(() -> {
                                                         assertThat(NMSClassRegistry.findClass(TestNMSClass.class)).isEqualTo(
                                                                 MockTestNMSClass.class);
                                                         return new Command("hello") {{
                                                             execute(ctx -> ctx.sendMessage("Hello!"));
                                                         }};
                                                     })
                                                     .permissionPrefix("test.command")
                                                     .build()) {
                FakeSender sender = FakeSender.player("Steve");
                tester.execute("hello", sender);
                assertThat(sender.getSentMessageTexts()).containsExactly("Hello!");
            }
        }
    }
}
