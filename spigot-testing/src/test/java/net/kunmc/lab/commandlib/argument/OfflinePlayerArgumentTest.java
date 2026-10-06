package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class OfflinePlayerArgumentTest {
    @Test
    void player_is_resolved_by_name_case_insensitively() {
        FakeSender sender = FakeSender.player("Alice");
        OfflinePlayer mockPlayer = Mockito.mock(OfflinePlayer.class);
        Mockito.when(mockPlayer.getName())
               .thenReturn("Steve");

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class); CommandTester tester = new CommandTester(
                new Command("info") {{
                    argument(new OfflinePlayerArgument("player")).execute((player, ctx) -> {
                        ctx.sendMessage(player.getName());
                    });
                }},
                "test.command")) {
            bukkit.when(Bukkit::getOfflinePlayers)
                  .thenReturn(new OfflinePlayer[]{mockPlayer});
            tester.execute("info steve", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Steve");
    }

    @Test
    void unknown_player_sends_failure_message() {
        FakeSender sender = FakeSender.player("Alice");

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class); CommandTester tester = new CommandTester(
                new Command("info") {{
                    argument(new OfflinePlayerArgument("player")).execute((player, ctx) -> {
                        ctx.sendMessage(player.getName());
                    });
                }},
                "test.command")) {
            bukkit.when(Bukkit::getOfflinePlayers)
                  .thenReturn(new OfflinePlayer[0]);
            tester.execute("info nobody", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("Incorrect argument for command",
                                                                 "info nobody<--[HERE]");
        assertThat(sender.getSentMessages()
                         .get(0)
                         .getColor()
                         .getColor()).isEqualTo(new java.awt.Color(0xFF5555));
    }
}
