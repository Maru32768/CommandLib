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

/**
 * Temporary audit probe (2026-11-03): whether a sender without the selector permission can use {@code @a} with the
 * offline player arguments on Paper. The assertion records the observation.
 */
class PbtAuditPaperScratchTest {
    @Test
    void h5_selector_without_the_selector_permission() {
        FakeSender sender = FakeSender.player("Guest")
                                      .denyPermissions("minecraft.command.selector");
        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
             CommandTester tester = CommandTester.builder()
                                                 .command(() -> new Command("players") {{
                                                     argument(new OfflinePlayersArgument("targets")).execute((ctx,
                                                                                                              targets) -> ctx.sendMessage("players:"
                                                                                                                      + targets.size()));
                                                 }})
                                                 .command(() -> new Command("uuids") {{
                                                     argument(new UUIDsArgument("targets")).execute((ctx,
                                                                                                     targets) -> ctx.sendMessage("uuids:"
                                                                                                             + targets.size()));
                                                 }})
                                                 .permissionPrefix("test.command")
                                                 .build()) {
            OfflinePlayer steve = Mockito.mock(OfflinePlayer.class);
            Mockito.when(steve.getName())
                   .thenReturn("Steve");
            Mockito.when(steve.getUniqueId())
                   .thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));
            bukkit.when(Bukkit::getOfflinePlayers)
                  .thenReturn(new OfflinePlayer[]{steve});
            tester.execute("players @a", sender);
            tester.execute("uuids @a", sender);
        }
        assertThat(sender.getSentMessageTexts()).as("H5 observed")
                                                .containsExactly("H5-EXPECTED-TO-FAIL");
    }
}
