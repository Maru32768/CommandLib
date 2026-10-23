package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UUIDArgumentTest {
    private static final UUID FIXED_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Test
    void raw_uuid_string_is_parsed() {
        FakeSender sender = FakeSender.player("Alice");

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(new Command("id") {{
                 argument(new UUIDArgument("uuid")).execute((ctx, uuid) -> {
                     ctx.sendMessage(uuid.toString());
                 });
             }}, "test.command")) {
            bukkit.when(Bukkit::getOfflinePlayers)
                  .thenReturn(new OfflinePlayer[0]);
            tester.execute("id " + FIXED_UUID, sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly(FIXED_UUID.toString());
    }

    @Test
    void player_name_resolves_to_that_players_uuid() {
        FakeSender sender = FakeSender.player("Alice");
        OfflinePlayer mockPlayer = Mockito.mock(OfflinePlayer.class);
        Mockito.when(mockPlayer.getName())
               .thenReturn("Steve");
        Mockito.when(mockPlayer.getUniqueId())
               .thenReturn(FIXED_UUID);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(new Command("id") {{
                 argument(new UUIDArgument("uuid")).execute((ctx, uuid) -> {
                     ctx.sendMessage(uuid.toString());
                 });
             }}, "test.command")) {
            bukkit.when(Bukkit::getOfflinePlayers)
                  .thenReturn(new OfflinePlayer[]{mockPlayer});
            tester.execute("id Steve", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly(FIXED_UUID.toString());
    }
}
