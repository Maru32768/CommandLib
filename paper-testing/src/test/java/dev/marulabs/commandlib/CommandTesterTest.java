package dev.marulabs.commandlib;

import com.mojang.brigadier.LiteralMessage;
import dev.marulabs.commandlib.argument.PlayerArgument;
import dev.marulabs.commandlib.argument.StringArgument;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class CommandTesterTest {
    @Nested
    class FakeSenderTests {
        @Test
        void player_is_instance_of_Player() {
            assertThat(FakeSender.player("Steve")
                                 .asSender()).isInstanceOf(Player.class);
        }

        @Test
        void console_is_instance_of_ConsoleCommandSender() {
            assertThat(FakeSender.console()
                                 .asSender()).isInstanceOf(ConsoleCommandSender.class);
        }

        @Test
        void player_name_is_set() {
            assertThat(FakeSender.player("Steve")
                                 .asSender()
                                 .getName()).isEqualTo("Steve");
        }

        @Test
        void player_locale_is_set() {
            assertThat(((Player) FakeSender.player("Steve")
                                           .locale("ja_jp")
                                           .asSender()).getLocale()).isEqualTo("ja_jp");
        }

        @Test
        void sent_messages_are_empty_before_execution() {
            assertThat(FakeSender.player("Steve")
                                 .getSentMessages()).isEmpty();
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
        void granted_and_denied_permissions_combine_and_a_new_grant_list_keeps_the_denied_nodes() {
            FakeSender sender = FakeSender.player("Steve")
                                          .permissions("a.b", "c.d")
                                          .denyPermissions("c.d");

            assertThat(sender.asSender()
                             .hasPermission("a.b")).isTrue();
            assertThat(sender.asSender()
                             .hasPermission("c.d")).isFalse();
            assertThat(sender.asSender()
                             .hasPermission("e.f")).isFalse();

            sender.permissions("c.d", "e.f");

            assertThat(sender.asSender()
                             .hasPermission("e.f")).isTrue();
            assertThat(sender.asSender()
                             .hasPermission("c.d")).isFalse();
            assertThat(sender.asSender()
                             .hasPermission("a.b")).isFalse();
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
    class ExecuteTests {
        @Test
        void message_is_captured() {
            CommandTester tester = new CommandTester(new Command("hello") {{
                execute(ctx -> ctx.sendMessage("Hello!"));
            }}, "test.command");
            FakeSender sender = FakeSender.player("Steve");

            tester.execute("hello", sender);

            assertThat(sender.getSentMessageTexts()).containsExactly("Hello!");
        }

        @Test
        void send_success_has_green_color() {
            CommandTester tester = new CommandTester(new Command("hello") {{
                execute(ctx -> ctx.sendSuccess("done"));
            }}, "test.command");
            FakeSender sender = FakeSender.player("Steve");

            tester.execute("hello", sender);

            assertThat(sender.getSentMessages()).extracting(net.kyori.adventure.text.Component::color)
                                                .containsExactly(NamedTextColor.GREEN);
        }

        @Test
        void builder_defaults_wrap_every_command_and_provide_values() {
            java.util.List<String> log = new java.util.ArrayList<>();
            CommandTester tester = CommandTester.builder()
                                                .command(new Command("a") {{
                                                    execute(ctx -> log.add("a " + ctx.get(String.class)));
                                                }})
                                                .command(new Command("b") {{
                                                    provide(String.class, ctx -> "b");
                                                    execute(ctx -> log.add("b " + ctx.get(String.class)));
                                                }})
                                                .permissionPrefix("test.command")
                                                .defaults(CommandDefaults.of(CommandContext.class)
                                                                         .addMiddleware((ctx, next) -> {
                                                                             log.add("audit " + ctx.getActor()
                                                                                                   .getName());
                                                                             return next.proceed();
                                                                         })
                                                                         .provide(String.class, ctx -> "default"))
                                                .build();
            FakeSender sender = FakeSender.player("Steve");

            tester.execute("a", sender);
            tester.execute("b", sender);

            assertThat(log).containsExactly("audit Steve", "a default", "audit Steve", "b b");
        }

        @Test
        void execute_returns_the_result() {
            CommandTester tester = new CommandTester(new Command("count") {{
                execute(ctx -> ctx.setResult(7));
            }}, "test.command");

            assertThat(tester.execute("count", FakeSender.player("Steve"))).isEqualTo(7);
        }

        @Test
        void failure_returns_zero_and_sends_its_messages() {
            CommandTester tester = new CommandTester(new Command("count") {{
                execute(ctx -> {
                    throw new CommandPrerequisiteException("no", "really");
                });
            }}, "test.command");
            FakeSender sender = FakeSender.player("Steve");

            assertThat(tester.execute("count", sender)).isZero();
            assertThat(sender.getSentMessageTexts()).containsExactly("no", "really");
        }

        @Test
        void broadcast_success_is_sent_to_the_sender() {
            CommandTester tester = new CommandTester(new Command("hello") {{
                execute(ctx -> ctx.sendSuccess("done", true));
            }}, "test.command");
            FakeSender sender = FakeSender.player("Steve");

            tester.execute("hello", sender);

            assertThat(sender.getSentMessages()).extracting(net.kyori.adventure.text.Component::color)
                                                .containsExactly(NamedTextColor.GREEN);
        }

        @Test
        void send_failure_has_red_color() {
            CommandTester tester = new CommandTester(new Command("hello") {{
                execute(ctx -> ctx.sendFailure("error"));
            }}, "test.command");
            FakeSender sender = FakeSender.player("Steve");

            tester.execute("hello", sender);

            assertThat(sender.getSentMessages()).extracting(net.kyori.adventure.text.Component::color)
                                                .containsExactly(NamedTextColor.RED);
        }

        @Test
        void multiple_messages_are_captured_in_order() {
            CommandTester tester = new CommandTester(new Command("hello") {{
                execute(ctx -> {
                    ctx.sendMessage("line1");
                    ctx.sendMessage("line2");
                    ctx.sendMessage("line3");
                });
            }}, "test.command");
            FakeSender sender = FakeSender.player("Steve");

            tester.execute("hello", sender);

            assertThat(sender.getSentMessageTexts()).containsExactly("line1", "line2", "line3");
        }

        @Test
        void command_with_string_argument() {
            CommandTester tester = new CommandTester(new Command("greet") {{
                argument(new StringArgument("name")).execute((name,
                                                              ctx) -> ctx.sendMessage("Hello, " + name + "!"));
            }}, "test.command");
            FakeSender sender = FakeSender.player("Steve");

            tester.execute("greet World", sender);

            assertThat(sender.getSentMessageTexts()).containsExactly("Hello, World!");
        }

        @Test
        void command_context_returns_player_language() {
            CommandTester tester = new CommandTester(new Command("locale") {{
                execute(ctx -> ctx.sendMessage(ctx.getLanguage()));
            }}, "test.command");
            FakeSender sender = FakeSender.player("Steve")
                                          .locale("ja_jp");

            tester.execute("locale", sender);

            assertThat(sender.getSentMessageTexts()).containsExactly("ja_jp");
        }

        @Test
        void command_context_returns_player_locale() {
            CommandTester tester = new CommandTester(new Command("locale") {{
                execute(ctx -> ctx.sendMessage(ctx.getLocale()
                                                  .toLanguageTag()));
            }}, "test.command");
            FakeSender sender = FakeSender.player("Steve")
                                          .locale("ja_jp");

            tester.execute("locale", sender);

            assertThat(sender.getSentMessageTexts()).containsExactly("ja-JP");
        }

        @Test
        void subcommand_message_is_captured() {
            CommandTester tester = new CommandTester(new Command("game") {{
                addChildren(new Command("start") {{
                    execute(ctx -> ctx.sendMessage("started"));
                }});
            }}, "test.command");
            FakeSender sender = FakeSender.player("Steve");

            tester.execute("game start", sender);

            assertThat(sender.getSentMessageTexts()).containsExactly("started");
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
    class SuggestionsTests {
        @Test
        void returns_suggestions_with_string_tooltip() throws Exception {
            CommandTester tester = new CommandTester(new Command("choose") {{
                argument(new StringArgument("value").addSuggestionProvider(sb -> sb.suggest("alpha",
                                                                                            "first value"))).execute((value,
                                                                                                                      ctx) -> ctx.sendSuccess(value));
            }}, "test.command");

            var suggestions = tester.suggestions("choose a", FakeSender.player("Steve"))
                                    .get();

            assertThat(suggestions.getList()).hasSize(1);
            assertThat(suggestions.getList()
                                  .get(0)
                                  .getText()).isEqualTo("alpha");
            assertThat(suggestions.getList()
                                  .get(0)
                                  .getTooltip()
                                  .getString()).isEqualTo("first value");
        }

        @Test
        void returns_suggestions_with_message_tooltip() throws Exception {
            CommandTester tester = new CommandTester(new Command("cmd") {{
                argument(new StringArgument("value").addSuggestionProvider(sb -> sb.suggest("alpha",
                                                                                            new LiteralMessage("alpha tooltip"))
                                                                                   .suggest("beta"))).execute((v,
                                                                                                               ctx) -> {
                                                                                   });
            }}, "test.command");
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

    @Nested
    class BuilderTests {
        @Test
        void command_can_be_registered_with_builder() {
            CommandTester tester = CommandTester.builder()
                                                .command(new Command("hello") {{
                                                    execute(ctx -> ctx.sendMessage("Hello!"));
                                                }})
                                                .permissionPrefix("test.command")
                                                .build();
            FakeSender sender = FakeSender.player("Steve");

            tester.execute("hello", sender);

            assertThat(sender.getSentMessageTexts()).containsExactly("Hello!");
        }
    }
}
