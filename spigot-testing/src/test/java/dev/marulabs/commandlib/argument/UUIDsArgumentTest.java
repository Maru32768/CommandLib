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

class UUIDsArgumentTest {
    private static final UUID FIXED_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Test
    void raw_uuid_string_resolves_to_single_element_list() {
        FakeSender sender = FakeSender.player("Alice");

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(() -> new Command("ids") {{
                 argument(new UUIDsArgument("uuids")).execute((ctx, uuids) -> {
                     ctx.sendMessage(uuids.size() + ":" + uuids.get(0));
                 });
             }}, "test.command")) {
            bukkit.when(Bukkit::getOfflinePlayers)
                  .thenReturn(new OfflinePlayer[0]);
            tester.execute("ids " + FIXED_UUID, sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("1:" + FIXED_UUID);
    }

    @Test
    void at_a_selector_returns_all_offline_players_uuids() {
        FakeSender sender = FakeSender.player("Alice");
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        OfflinePlayer p1 = Mockito.mock(OfflinePlayer.class);
        OfflinePlayer p2 = Mockito.mock(OfflinePlayer.class);
        Mockito.when(p1.getUniqueId())
               .thenReturn(uuid1);
        Mockito.when(p2.getUniqueId())
               .thenReturn(uuid2);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = new CommandTester(() -> new Command("ids") {{
                 argument(new UUIDsArgument("uuids")).execute((ctx, uuids) -> {
                     ctx.sendMessage(String.valueOf(uuids.size()));
                 });
             }}, "test.command")) {
            bukkit.when(Bukkit::getOfflinePlayers)
                  .thenReturn(new OfflinePlayer[]{p1, p2});
            tester.execute("ids @a", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("2");
    }
}
