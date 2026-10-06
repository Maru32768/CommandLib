package net.kunmc.lab.commandlib;

import com.mojang.brigadier.suggestion.Suggestion;
import net.kunmc.lab.commandlib.argument.*;
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
        void unknown_command_throws() {
            try (CommandTester tester = new CommandTester(new Command("hello") {{
                execute(ctx -> {
                });
            }}, "test")) {
                assertThatThrownBy(() -> tester.execute("unknown", FakeSender.player("Steve"))).isInstanceOf(
                        RuntimeException.class);
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
    class Arguments {
        private <T> T parse(CommonArgument<T, CommandContext, ?> argument, String input, FakeSender sender) {
            AtomicReference<T> result = new AtomicReference<>();
            try (CommandTester tester = new CommandTester(new Command("arg") {{
                argument(argument).execute((a, ctx) -> result.set(a));
            }}, "test")) {
                tester.execute("arg " + input, sender);
            }
            return result.get();
        }

        @Test
        void item_stack() {
            assertThat(parse(new ItemStackArgument("a"), "minecraft:diamond", FakeSender.console()).getItem())
                    .isEqualTo(Items.DIAMOND);
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
                                             .registryOrThrow(Registries.ENCHANTMENT)
                                             .getHolderOrThrow(Enchantments.SHARPNESS)));
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
                                                                                                    .getBlock())
                    .isEqualTo(Blocks.STONE);
        }

        @Test
        void block_pos() {
            assertThat(parse(new BlockPosArgument("a"), "1 2 3", FakeSender.console()).getY()).isEqualTo(2);
        }

        @Test
        void player_by_name() {
            AtomicReference<String> result = new AtomicReference<>();
            try (CommandTester tester = new CommandTester(new Command("arg") {{
                argument(new PlayerArgument("a")).execute((a, ctx) -> result.set(a.getGameProfile()
                                                                                    .getName()));
            }}, "test").withPlayer(FakeSender.player("Alex"))) {
                tester.execute("arg Alex",
                               FakeSender.player("Steve")
                                         .op(true));
            }
            assertThat(result.get()).isEqualTo("Alex");
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
                argument(new StringArgument("a").addSuggestionAction(builder -> {
                    builder.suggest("alpha");
                    builder.suggest("beta");
                })).execute((a, ctx) -> {
                });
            }}, "test")) {
                assertThat(suggestionTexts(tester, "suggest ")).contains("alpha", "beta");
            }
        }

        @Test
        void online_player_names_are_suggested_for_game_profiles() {
            try (CommandTester tester = new CommandTester(new Command("profile") {{
                argument(new GameProfileArgument("a")).execute((a, ctx) -> {
                });
            }}, "test").withPlayer(FakeSender.player("Alex"))) {
                assertThat(suggestionTexts(tester, "profile ")).contains("Alex", "Steve");
            }
        }
    }
}
