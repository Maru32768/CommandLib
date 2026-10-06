package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Selector and failure behaviour shared by the entity and player arguments.
 */
class EntitySelectorArgumentTest {
    @Test
    void self_selector_resolves_to_sender() {
        FakeSender steve = FakeSender.player("Steve");

        try (CommandTester tester = tester()) {
            tester.execute("player @s", steve);
            tester.execute("entity @s", steve);
        }

        assertThat(steve.getSentMessageTexts()).containsExactly("player=Steve", "entity=Steve");
    }

    @Test
    void self_selector_without_entity_sender_finds_nothing() {
        FakeSender console = FakeSender.console();

        try (CommandTester tester = tester()) {
            tester.execute("player @s", console);
            tester.execute("entity @s", console);
        }

        assertThat(console.getSentMessageTexts()).containsExactly("No player was found", "No entity was found");
    }

    @Test
    void all_players_selector_resolves_every_registered_player() {
        FakeSender admin = FakeSender.player("Admin");
        FakeSender alex = FakeSender.player("Alex");
        FakeSender steve = FakeSender.player("Steve");

        try (CommandTester tester = tester()) {
            tester.withFakePlayer((Player) alex.asSender())
                  .withFakePlayer((Player) steve.asSender())
                  .withFakeEntity("zombie", namedEntity("zombie"));
            tester.execute("players @a", admin);
            tester.execute("entities @e", admin);
        }

        assertThat(admin.getSentMessageTexts()).containsExactly("players=[Alex, Steve]",
                                                                "entities=[Alex, Steve, zombie]");
    }

    @Test
    void nearest_player_selector_resolves_one_player() {
        FakeSender admin = FakeSender.player("Admin");
        FakeSender alex = FakeSender.player("Alex");

        try (CommandTester tester = tester()) {
            tester.withFakeEntity("zombie", namedEntity("zombie"))
                  .withFakePlayer((Player) alex.asSender());
            tester.execute("player @p", admin);
        }

        assertThat(admin.getSentMessageTexts()).containsExactly("player=Alex");
    }

    @Test
    void single_target_arguments_reject_multi_target_selectors_while_parsing() {
        try (CommandTester tester = tester()) {
            assertSyntaxError(tester, "player @a");
            assertSyntaxError(tester, "entity @e");
        }
    }

    @Test
    void player_arguments_reject_entity_selector_while_parsing() {
        try (CommandTester tester = tester()) {
            assertSyntaxError(tester, "players @e");
        }
    }

    @Test
    void missing_targets_send_failure_messages() {
        FakeSender admin = FakeSender.player("Admin");

        try (CommandTester tester = tester()) {
            tester.execute("entity nobody", admin);
            tester.execute("entities @e", admin);
            tester.execute("player nobody", admin);
            tester.execute("players @a", admin);
        }

        assertThat(admin.getSentMessageTexts()).containsExactly("No entity was found",
                                                                "No entity was found",
                                                                "No player was found",
                                                                "No player was found");
    }

    @Test
    void player_argument_does_not_accept_non_player_entity_name() {
        FakeSender admin = FakeSender.player("Admin");

        try (CommandTester tester = tester()) {
            tester.withFakeEntity("zombie", namedEntity("zombie"));
            tester.execute("player zombie", admin);
        }

        assertThat(admin.getSentMessageTexts()).containsExactly("No player was found");
    }

    @Test
    void selector_arguments_are_reported_as_unsupported_by_the_mock() {
        try (CommandTester tester = tester()) {
            // Brigadier wraps exceptions thrown while parsing into a syntax error.
            assertThatThrownBy(() -> tester.execute("entities @e[type=zombie]",
                                                    FakeSender.player("Admin"))).hasMessageContaining(
                    "Selector arguments are not supported by the test mock");
        }
    }

    private static void assertSyntaxError(CommandTester tester, String input) {
        assertThatThrownBy(() -> tester.execute(input, FakeSender.player("Admin"))).as(input)
                                                                                   .hasCauseInstanceOf(
                                                                                           CommandSyntaxException.class);
    }

    private static Entity namedEntity(String name) {
        Entity entity = Mockito.mock(Entity.class);
        Mockito.when(entity.getName())
               .thenReturn(name);
        return entity;
    }

    private static CommandTester tester() {
        // Commands are created through suppliers so the NMS argument mocks are active while they are constructed.
        return CommandTester.builder()
                            .command(() -> new Command("entity") {{
                                argument(new EntityArgument("target")).execute((target, ctx) -> ctx.sendMessage(
                                        "entity=" + target.getName()));
                            }})
                            .command(() -> new Command("entities") {{
                                argument(new EntitiesArgument("targets")).execute((targets, ctx) -> ctx.sendMessage(
                                        "entities=" + names(targets)));
                            }})
                            .command(() -> new Command("player") {{
                                argument(new PlayerArgument("target")).execute((target, ctx) -> ctx.sendMessage(
                                        "player=" + target.getName()));
                            }})
                            .command(() -> new Command("players") {{
                                argument(new PlayersArgument("targets")).execute((targets, ctx) -> ctx.sendMessage(
                                        "players=" + names(targets)));
                            }})
                            .permissionPrefix("test.command")
                            .build();
    }

    private static String names(List<? extends Entity> entities) {
        return entities.stream()
                       .map(Entity::getName)
                       .collect(Collectors.toList())
                       .toString();
    }
}
