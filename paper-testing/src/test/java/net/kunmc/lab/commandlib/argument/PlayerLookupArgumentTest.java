package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Selector and name lookup behaviour of the offline player and UUID arguments, matching the Spigot implementation.
 */
class PlayerLookupArgumentTest {
    private static final UUID STEVE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ALEX_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void all_selector_returns_every_offline_player() {
        FakeSender sender = FakeSender.player("Admin");

        run(sender, new OfflinePlayer[]{player("Steve", STEVE_ID), player("Alex", ALEX_ID)}, "players @a", "uuids @a");

        assertThat(sender.getSentMessageTexts()).containsExactly("2:[Steve, Alex]",
                                                                 "2:[" + STEVE_ID + ", " + ALEX_ID + "]");
    }

    @Test
    void random_selector_returns_one_player() {
        FakeSender sender = FakeSender.player("Admin");

        run(sender, new OfflinePlayer[]{player("Steve", STEVE_ID), player("Alex", ALEX_ID)}, "players @r", "uuids @r");

        assertThat(sender.getSentMessageTexts()).hasSize(2)
                                                .allMatch(x -> x.startsWith("1:"));
    }

    @Test
    void selectors_without_players_send_failure() {
        FakeSender sender = FakeSender.player("Admin");

        run(sender, new OfflinePlayer[0], "players @a", "uuids @r");

        assertThat(sender.getSentMessageTexts()).containsExactly("no player found.", "no player found");
    }

    @Test
    void unsupported_selectors_send_failure() {
        FakeSender sender = FakeSender.player("Admin");

        run(sender, new OfflinePlayer[]{player("Steve", STEVE_ID)}, "players @p", "uuids @s");

        assertThat(sender.getSentMessageTexts()).containsExactly("@p is invalid selector.", "@s is invalid selector");
    }

    @Test
    void names_are_matched_case_insensitively() {
        FakeSender sender = FakeSender.player("Admin");

        run(sender,
            new OfflinePlayer[]{player("Steve", STEVE_ID)},
            "players steve",
            "uuids STEVE",
            "uuid sTeVe",
            "offline STEVE");

        assertThat(sender.getSentMessageTexts()).containsExactly("1:[Steve]",
                                                                 "1:[" + STEVE_ID + "]",
                                                                 STEVE_ID.toString(),
                                                                 "Steve");
    }

    @Test
    void unknown_names_send_failure() {
        FakeSender sender = FakeSender.player("Admin");

        run(sender, new OfflinePlayer[]{player("Steve", STEVE_ID)}, "uuids nobody", "uuid nobody");

        assertThat(sender.getSentMessageTexts()).containsExactly("nobody is not found or not valid UUID",
                                                                 "nobody is not found or not valid UUID");
    }

    @Test
    void raw_uuid_of_unknown_player_is_accepted() {
        FakeSender sender = FakeSender.player("Admin");
        UUID unknown = UUID.randomUUID();

        run(sender, new OfflinePlayer[0], "uuids " + unknown, "uuid " + unknown);

        assertThat(sender.getSentMessageTexts()).containsExactly("1:[" + unknown + "]", unknown.toString());
    }

    private static void run(FakeSender sender, OfflinePlayer[] offlinePlayers, String... inputs) {
        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = CommandTester.builder()
                                                 .command(() -> new Command("players") {{
                                                     argument(new OfflinePlayersArgument("targets")).execute((targets,
                                                                                                              ctx) -> ctx.sendMessage(
                                                             targets.size() + ":" + targets.stream()
                                                                                           .map(OfflinePlayer::getName)
                                                                                           .collect(Collectors.toList())));
                                                 }})
                                                 .command(() -> new Command("uuids") {{
                                                     argument(new UUIDsArgument("targets")).execute((targets, ctx) -> ctx.sendMessage(
                                                             targets.size() + ":" + targets));
                                                 }})
                                                 .command(() -> new Command("uuid") {{
                                                     argument(new UUIDArgument("target")).execute((target, ctx) -> ctx.sendMessage(
                                                             target.toString()));
                                                 }})
                                                 .command(() -> new Command("offline") {{
                                                     argument(new OfflinePlayerArgument("target")).execute((target, ctx) -> ctx.sendMessage(
                                                             target.getName()));
                                                 }})
                                                 .permissionPrefix("test.command")
                                                 .build()) {
            bukkit.when(Bukkit::getOfflinePlayers)
                  .thenReturn(offlinePlayers);
            for (String input : inputs) {
                tester.execute(input, sender);
            }
        }
    }

    private static OfflinePlayer player(String name, UUID uuid) {
        OfflinePlayer player = Mockito.mock(OfflinePlayer.class);
        Mockito.when(player.getName())
               .thenReturn(name);
        Mockito.when(player.getUniqueId())
               .thenReturn(uuid);
        return player;
    }
}
