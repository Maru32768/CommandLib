package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class OfflinePlayersArgumentTest {
    @Test
    void player_name_resolves_to_single_element_list() {
        FakeSender sender = FakeSender.player("Alice");
        OfflinePlayer mockPlayer = Mockito.mock(OfflinePlayer.class);
        Mockito.when(mockPlayer.getName())
               .thenReturn("Steve");

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(() -> new Command("heal") {{
                 argument(new OfflinePlayersArgument("targets")).execute((ctx, targets) -> {
                     ctx.sendMessage(targets.size() + ":" + targets.get(0)
                                                                   .getName());
                 });
             }}, "test.command")) {
            bukkit.when(Bukkit::getOfflinePlayers)
                  .thenReturn(new OfflinePlayer[]{mockPlayer});
            tester.execute("heal Steve", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("1:Steve");
    }

    @Test
    void at_a_selector_returns_all_offline_players() {
        FakeSender sender = FakeSender.player("Alice");
        OfflinePlayer p1 = Mockito.mock(OfflinePlayer.class);
        OfflinePlayer p2 = Mockito.mock(OfflinePlayer.class);
        Mockito.when(p1.getName())
               .thenReturn("PlayerA");
        Mockito.when(p2.getName())
               .thenReturn("PlayerB");

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(() -> new Command("heal") {{
                 argument(new OfflinePlayersArgument("targets")).execute((ctx, targets) -> {
                     ctx.sendMessage(String.valueOf(targets.size()));
                 });
             }}, "test.command")) {
            bukkit.when(Bukkit::getOfflinePlayers)
                  .thenReturn(new OfflinePlayer[]{p1, p2});
            tester.execute("heal @a", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("2");
    }
}
