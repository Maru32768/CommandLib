package dev.marulabs.commandlib;

import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestion;
import dev.marulabs.commandlib.argument.*;
import dev.marulabs.commandlib.util.text.ClickAction;
import dev.marulabs.commandlib.util.text.RichText;
import dev.marulabs.commandlib.util.text.RichTexts;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
//? if >=1.20.5 {
/*import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;
*///?}
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandTesterTest {
    /**
     * Throws a Brigadier exception carrying the input while resolving, as a custom argument type may.
     */
    static final class ResolvingArgument extends Argument<String, ResolvingArgument> {
        ResolvingArgument(String name) {
            super(name, StringArgumentType.word());
        }

        @Override
        public String cast(Object parsedArgument) {
            return (String) parsedArgument;
        }

        @Override
        protected String parseImpl(CommandContext ctx) throws CommandSyntaxException {
            StringReader reader = new StringReader(ctx.getHandle()
                                                      .getInput());
            reader.setCursor(reader.getString()
                                   .indexOf(' ')
                    + 1);
            throw new SimpleCommandExceptionType(new LiteralMessage("Bad value")).createWithContext(reader);
        }
    }

    @Nested
    class Execute {
        @Test
        void message_is_captured_for_player() {
            try (CommandTester tester = new CommandTester(new Command("hello") {{
                execute(ctx -> ctx.sendMessage("Hello!"));
            }}, "test")) {
                FakeSender sender = FakeSender.player("Steve");
                tester.execute("hello", sender);
                assertThat(sender.getSentMessageTexts()).containsExactly("Hello!");
            }
        }

        @Test
        void message_is_captured_for_console() {
            try (CommandTester tester = new CommandTester(new Command("hello") {{
                execute(ctx -> ctx.sendSuccess("done"));
            }}, "test")) {
                FakeSender sender = FakeSender.console();
                tester.execute("hello", sender);
                assertThat(sender.getSentMessageTexts()).containsExactly("done");
            }
        }

        @Test
        void execute_returns_the_result() {
            try (CommandTester tester = new CommandTester(new Command("count") {{
                execute(ctx -> ctx.setResult(7));
            }}, "test")) {
                assertThat(tester.execute("count", FakeSender.player("Steve"))).isEqualTo(7);
            }
        }

        @Test
        void failure_returns_zero_and_sends_its_messages() {
            try (CommandTester tester = new CommandTester(new Command("count") {{
                execute(ctx -> {
                    throw new CommandPrerequisiteException("no", "really");
                });
            }}, "test")) {
                FakeSender sender = FakeSender.player("Steve");
                assertThat(tester.execute("count", sender)).isZero();
                assertThat(sender.getSentMessageTexts()).containsExactly("no", "really");
            }
        }

        @Test
        void a_failed_assertion_fails_execute_when_the_error_formatter_sends_nothing() {
            try (CommandTester tester = new CommandTester(new Command("check") {{
                errorFormatter((ctx, error) -> {
                });
                execute(ctx -> {
                    throw new AssertionError("expected");
                });
            }}, "test")) {
                FakeSender sender = FakeSender.player("Steve");

                assertThatThrownBy(() -> tester.execute("check", sender)).isInstanceOf(AssertionError.class)
                                                                         .hasMessage("expected");
                assertThat(sender.getSentMessageTexts()).isEmpty();
            }
        }

        @Test
        void rich_success_is_sent_as_feedback() {
            try (CommandTester tester = new CommandTester(new Command("hello") {{
                execute(ctx -> ctx.sendSuccess(RichText.text("done")));
            }}, "test")) {
                FakeSender sender = FakeSender.console();
                tester.execute("hello", sender);
                assertThat(sender.getSentMessageTexts()).containsExactly("done");
            }
        }

        @Test
        void failure_message_is_captured() {
            try (CommandTester tester = new CommandTester(new Command("hello") {{
                execute(ctx -> ctx.sendFailure("failed"));
            }}, "test")) {
                FakeSender sender = FakeSender.player("Steve");
                tester.execute("hello", sender);
                assertThat(sender.getSentMessageTexts()).containsExactly("failed");
            }
        }

        @Test
        void rich_text_is_sent_as_vanilla_component() {
            try (CommandTester tester = new CommandTester(new Command("hello") {{
                execute(ctx -> {
                    ctx.sendMessage(RichText.text("hi")
                                            .color(0x123456)
                                            .click(ClickAction.runCommand("/hello")));
                    ctx.sendSuccess(RichText.text("ok"));
                    ctx.sendFailure(RichText.text("failed"));
                });
            }}, "test")) {
                FakeSender sender = FakeSender.player("Steve");
                tester.execute("hello", sender);
                assertThat(sender.getSentMessageTexts()).containsExactly("hi", "ok", "failed");
                assertThat(sender.getSentMessages()
                                 .get(0)
                                 .getStyle()
                                 .getClickEvent()
                                 .getValue()).isEqualTo("/hello");
                assertThat(sender.getSentMessages()
                                 .get(1)
                                 .getStyle()
                                 .getColor()).isEqualTo(TextColor.fromLegacyFormat(ChatFormatting.GREEN));
            }
        }

        @Test
        void colored_component_sends_do_not_change_the_passed_component() {
            MutableComponent plain = RichTexts.toVanilla(RichText.text("plain"));
            MutableComponent colored = RichTexts.toVanilla(RichText.text("colored")
                                                                   .color(0x55FFFF));
            try (CommandTester tester = new CommandTester(new Command("hello") {{
                execute(ctx -> {
                    ctx.sendSuccess(plain);
                    ctx.sendWarn(colored);
                });
            }}, "test")) {
                FakeSender sender = FakeSender.console();
                tester.execute("hello", sender);
                assertThat(sender.getSentMessages()).extracting(c -> c.getStyle()
                                                                      .getColor())
                                                    .containsExactly(TextColor.fromLegacyFormat(ChatFormatting.GREEN),
                                                                     TextColor.fromLegacyFormat(ChatFormatting.AQUA));
                assertThat(plain.getStyle()
                                .getColor()).isNull();
            }
        }

        @Test
        void unknown_command_throws() {
            try (CommandTester tester = new CommandTester(new Command("hello") {{
                execute(ctx -> {
                });
            }}, "test")) {
                assertThatThrownBy(() -> tester.execute("unknown",
                                                        FakeSender.player("Steve"))).isInstanceOf(RuntimeException.class);
            }
        }

        @Test
        void actor_describes_player() {
            AtomicReference<String> result = new AtomicReference<>();
            try (CommandTester tester = new CommandTester(new Command("whoami") {{
                execute(ctx -> {
                    CommandActor actor = ctx.getActor();
                    result.set(actor.getName() + "," + actor.getType() + "," + actor.getUniqueId()
                                                                                    .orElse(null));
                });
            }}, "test")) {
                UUID uuid = UUID.randomUUID();
                tester.execute("whoami", FakeSender.player("Steve", uuid));
                assertThat(result.get()).isEqualTo("Steve,PLAYER," + uuid);
            }
        }
    }

    @Nested
    class Permissions {
        private Command command(List<String> executed) {
            return new Command("secret") {{
                execute(ctx -> executed.add("secret"));
            }};
        }

        @Test
        void sender_has_every_node_by_default() {
            List<String> executed = new ArrayList<>();
            try (CommandTester tester = new CommandTester(command(executed), "test")) {
                tester.execute("secret", FakeSender.player("Steve"));
            }
            assertThat(executed).containsExactly("secret");
        }

        @Test
        void granted_node_allows_execution() {
            List<String> executed = new ArrayList<>();
            try (CommandTester tester = new CommandTester(command(executed), "test")) {
                tester.execute("secret",
                               FakeSender.player("Steve")
                                         .permissions("test.secret"));
            }
            assertThat(executed).containsExactly("secret");
        }

        @Test
        void denied_node_rejects_command() {
            List<String> executed = new ArrayList<>();
            try (CommandTester tester = new CommandTester(command(executed), "test")) {
                FakeSender sender = FakeSender.player("Steve")
                                              .denyPermissions("test.secret");
                assertThatThrownBy(() -> tester.execute("secret", sender)).isInstanceOf(RuntimeException.class);
            }
            assertThat(executed).isEmpty();
        }
    }

    @Nested
    class Builders {
        @Test
        void player_builder_sets_every_attribute() {
            AtomicReference<String> result = new AtomicReference<>();
            try (CommandTester tester = new CommandTester(new Command("whoami") {{
                execute(ctx -> result.set(ctx.getActor()
                                             .getName()
                        + "," + ctx.getActor()
                                   .getUniqueId()
                                   .orElse(null)));
            }}, "test")) {
                UUID uuid = UUID.randomUUID();
                FakeSender sender = FakeSender.playerBuilder("Alex")
                                              .name("Steve")
                                              .uniqueId(uuid)
                                              .op(true)
                                              .permissions("test.whoami", "a.b")
                                              .denyPermissions("a.b")
                                              .build();
                tester.execute("whoami", sender);

                assertThat(result.get()).isEqualTo("Steve," + uuid);
                assertThat(sender.isPlayer()).isTrue();
                assertThat(sender.isOp()).isTrue();
                assertThat(sender.hasPermission("a.b")).isFalse();
                assertThat(sender.hasPermission("c.d")).isFalse();
            }
        }

        @Test
        void console_builder_builds_an_operator_console_that_passes_every_check() {
            FakeSender sender = FakeSender.consoleBuilder()
                                          .build();

            assertThat(sender.getName()).isEqualTo("Server");
            assertThat(sender.isPlayer()).isFalse();
            assertThat(sender.isOp()).isTrue();
            assertThat(sender.hasPermission("c.d")).isTrue();
        }

        @Test
        void console_permissions_are_refused_since_the_console_passes_every_check() {
            assertThatThrownBy(() -> FakeSender.consoleBuilder()
                                               .denyPermissions("a.b")
                                               .build()).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> FakeSender.consoleBuilder()
                                               .permissions("a.b")
                                               .build()).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> FakeSender.console()
                                               .permissions("a.b")).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> FakeSender.console()
                                               .denyPermissions("a.b")).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void granted_and_denied_permissions_combine_and_a_new_grant_list_keeps_the_denied_nodes() {
            FakeSender sender = FakeSender.player("Steve")
                                          .permissions("a.b", "c.d")
                                          .denyPermissions("c.d");

            assertThat(sender.hasPermission("a.b")).isTrue();
            assertThat(sender.hasPermission("c.d")).isFalse();
            assertThat(sender.hasPermission("e.f")).isFalse();

            sender.permissions("c.d", "e.f");

            assertThat(sender.hasPermission("e.f")).isTrue();
            assertThat(sender.hasPermission("c.d")).isFalse();
            assertThat(sender.hasPermission("a.b")).isFalse();
        }

        @Test
        void duplicate_permission_nodes_are_accepted() {
            assertThat(FakeSender.player("Steve")
                                 .permissions("a.b", "a.b")
                                 .hasPermission("a.b")).isTrue();
            assertThat(FakeSender.player("Alex")
                                 .denyPermissions("a.b", "a.b")
                                 .hasPermission("a.b")).isFalse();
            assertThat(FakeSender.playerBuilder("Bob")
                                 .permissions("a.b", "a.b")
                                 .build()
                                 .hasPermission("a.b")).isTrue();
        }
    }

    @Nested
    class Arguments {
        private <T> T parse(CommonArgument<T, CommandContext, ?> argument, String input, FakeSender sender) {
            AtomicReference<T> result = new AtomicReference<>();
            try (CommandTester tester = new CommandTester(new Command("arg") {{
                argument(argument).execute((ctx, a) -> result.set(a));
            }}, "test")) {
                tester.execute("arg " + input, sender);
            }
            return result.get();
        }

        @Test
        void item_stack() {
            assertThat(parse(new ItemStackArgument("a"),
                             "minecraft:diamond",
                             FakeSender.console()).getItem()).isEqualTo(Items.DIAMOND);
        }

        @Test
        void effect() {
            assertThat(parse(new EffectArgument("a"), "minecraft:speed", FakeSender.console()))
                                                                                               //? if >=1.20.5 {
                                                                                               /*.isSameAs(MobEffects.MOVEMENT_SPEED);
                                                                                               *///?} else
                                                                                               .isEqualTo(MobEffects.MOVEMENT_SPEED);
        }

        @Test
        void enchantment() {
            //? if >=1.20.5 {
            /*assertThat(parse(new EnchantmentArgument("a"), "minecraft:sharpness", FakeSender.console())
                               .is(Enchantments.SHARPNESS)).isTrue();
            *///?} else {
            Enchantment enchantment = parse(new EnchantmentArgument("a"), "minecraft:sharpness", FakeSender.console());
            assertThat(enchantment.getDescriptionId()).isEqualTo("enchantment.minecraft.sharpness");
            //?}
        }

        //? if >=1.20.5 {
        /*@Test
        void registry_access_resolves_data_driven_registries() {
            AtomicReference<Holder<Enchantment>> result = new AtomicReference<>();
            try (CommandTester tester = new CommandTester(new Command("arg") {{
                execute(ctx -> result.set(ctx.getHandle()
                                             .getSource()
                                             .registryAccess()
                                             .lookupOrThrow(Registries.ENCHANTMENT)
                                             .getOrThrow(Enchantments.SHARPNESS)));
            }}, "test")) {
                tester.execute("arg", FakeSender.console());
            }
            assertThat(result.get()
                             .is(Enchantments.SHARPNESS)).isTrue();
        }
        *///?}

        @Test
        void block_state() {
            assertThat(parse(new BlockStateArgument("a"), "minecraft:stone", FakeSender.console()).getState()
                                                                                                  .getBlock()).isEqualTo(Blocks.STONE);
        }

        @Test
        void block_pos() {
            assertThat(parse(new BlockPositionArgument("a"), "1 2 3", FakeSender.console()).getY()).isEqualTo(2);
        }

        @Test
        void player_by_name() {
            AtomicReference<String> result = new AtomicReference<>();
            try (CommandTester tester = new CommandTester(new Command("arg") {{
                argument(new PlayerArgument("a")).execute((ctx, a) -> result.set(a.getGameProfile()
                                                                                  .getName()));
            }}, "test").withPlayer(FakeSender.player("Alex"))) {
                tester.execute("arg Alex",
                               FakeSender.player("Steve")
                                         .op(true));
            }
            assertThat(result.get()).isEqualTo("Alex");
        }

        @Test
        void vanilla_error_while_resolving_is_sent_as_failure() {
            FakeSender sender = FakeSender.player("Steve")
                                          .op(true);
            try (CommandTester tester = new CommandTester(new Command("arg") {{
                argument(new PlayerArgument("a")).execute((ctx, a) -> ctx.sendMessage("unreachable"));
            }}, "test")) {
                // The selector parses, but no online player has the name, so resolving it fails.
                tester.execute("arg Nobody", sender);
            }
            assertThat(sender.getSentMessageTexts()).containsExactly("No player was found");
            assertThat(sender.getSentMessages()
                             .get(0)
                             .getStyle()
                             .getColor()).isEqualTo(TextColor.fromLegacyFormat(ChatFormatting.RED));
        }

        @Test
        void selector_matching_nothing_is_rejected_by_every_selector_argument() {
            FakeSender sender = FakeSender.player("Steve")
                                          .op(true);
            try (CommandTester tester = new CommandTester(new Command("arg") {{
                addChildren(new Command("players") {{
                    argument(new PlayersArgument("p")).execute((ctx, p) -> ctx.sendMessage("unreachable"));
                }}, new Command("entities") {{
                    argument(new EntitiesArgument("e")).execute((ctx, e) -> ctx.sendMessage("unreachable"));
                }});
            }}, "test")) {
                // A name selector parses, and vanilla's getPlayers / getEntities reject the empty result.
                tester.execute("arg players Nobody", sender);
                tester.execute("arg entities Nobody", sender);
            }
            assertThat(sender.getSentMessageTexts()).containsExactly("No player was found", "No entity was found");
        }

        // One node would parse both branches with the vanilla EntityArgument of one of them.
        @Test
        void player_and_entities_arguments_cannot_share_a_node() {
            assertThatThrownBy(() -> new CommandTester(new Command("x") {{
                argument(new PlayerArgument("target")).execute((ctx, target) -> {
                });
                argument(new EntitiesArgument("target"),
                         new StringArgument("reason")).execute((ctx, target, reason) -> {
                         });
            }}, "test")).hasMessageContaining("parse differently");
        }

        @Test
        void syntax_exception_with_input_sends_the_context_line() {
            FakeSender sender = FakeSender.console();
            try (CommandTester tester = new CommandTester(new Command("check") {{
                argument(new ResolvingArgument("value")).execute((ctx, value) -> ctx.sendMessage("unreachable"));
            }}, "test")) {
                tester.execute("check bad", sender);
            }
            assertThat(sender.getSentMessageTexts()).containsExactly("Bad value", "§7check §c§nbad§r<--[HERE]");
        }

        @Test
        void game_profile_and_uuid_by_name() {
            FakeSender steve = FakeSender.player("Steve");
            UUID uuid = steve.getUniqueId()
                             .orElseThrow();
            assertThat(parse(new GameProfileArgument("a"), "Steve", steve).getId()).isEqualTo(uuid);
            assertThat(parse(new UUIDArgument("a"), "Steve", steve)).isEqualTo(uuid);
        }
    }

    @Nested
    class Suggestions {
        private List<String> suggestionTexts(CommandTester tester, String input) {
            return tester.suggestions(input, FakeSender.player("Steve"))
                         .join()
                         .getList()
                         .stream()
                         .map(Suggestion::getText)
                         .collect(Collectors.toList());
        }

        @Test
        void suggestion_action_results_are_returned() {
            try (CommandTester tester = new CommandTester(new Command("suggest") {{
                argument(new StringArgument("a").addSuggestionProvider(builder -> {
                    builder.suggest("alpha");
                    builder.suggest("beta");
                })).execute((ctx, a) -> {
                });
            }}, "test")) {
                assertThat(suggestionTexts(tester, "suggest ")).contains("alpha", "beta");
            }
        }

        @Test
        void online_player_names_are_suggested_for_game_profiles() {
            try (CommandTester tester = new CommandTester(new Command("profile") {{
                argument(new GameProfileArgument("a")).execute((ctx, a) -> {
                });
            }}, "test").withPlayer(FakeSender.player("Alex"))) {
                assertThat(suggestionTexts(tester, "profile ")).contains("Alex", "Steve");
            }
        }

        @Test
        void suggestions_leave_out_children_the_sender_cannot_use() {
            // Brigadier's server-side completion would also list "stop"; the client's command tree hides it.
            try (CommandTester tester = new CommandTester(new Command("game") {{
                addChildren(new Command("start") {{
                    execute(ctx -> ctx.sendMessage("start"));
                }}, new Command("stop") {{
                    execute(ctx -> ctx.sendMessage("stop"));
                }});
            }}, "test.command")) {
                FakeSender sender = FakeSender.player("Steve")
                                              .permissions("test.command.game", "test.command.game.start");

                assertThat(tester.suggest("game ", sender)).containsExactly("help", "start");
                assertThat(tester.suggest("game s", sender)).containsExactly("start");
                assertThat(tester.suggestions("game ", sender)
                                 .join()
                                 .getList()).hasSize(2);
            }
        }
    }
}
