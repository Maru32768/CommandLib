package net.kunmc.lab.commandlib.util.bukkit;

import org.assertj.core.api.Assertions;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

class BukkitUtilTest {
    @Test
    void getMinecraftVersion() {
        Map<String, String> bukkitVersionToExpected = new HashMap<>();
        bukkitVersionToExpected.put("1.20-R0.1-SNAPSHOT", "1.20");
        bukkitVersionToExpected.put("1.20.1-R0.1-SNAPSHOT", "1.20.1");

        try (MockedStatic<Bukkit> mockedStatic = Mockito.mockStatic(Bukkit.class)) {
            for (Map.Entry<String, String> entry : bukkitVersionToExpected.entrySet()) {
                String bukkitVersion = entry.getKey();
                String expected = entry.getValue();

                mockedStatic.when(Bukkit::getBukkitVersion)
                            .thenReturn(bukkitVersion);

                String version = BukkitUtil.getMinecraftVersion();
                Assertions.assertThat(version)
                          .isEqualTo(expected);
            }
        }
    }

    @Test
    void offline_player_lookup_ignores_case_and_players_without_name() {
        org.bukkit.OfflinePlayer unnamed = Mockito.mock(org.bukkit.OfflinePlayer.class);
        org.bukkit.OfflinePlayer steve = Mockito.mock(org.bukkit.OfflinePlayer.class);
        Mockito.when(steve.getName())
               .thenReturn("Steve");

        try (MockedStatic<Bukkit> mockedStatic = Mockito.mockStatic(Bukkit.class)) {
            mockedStatic.when(Bukkit::getOfflinePlayers)
                        .thenReturn(new org.bukkit.OfflinePlayer[]{unnamed, steve});

            Assertions.assertThat(BukkitUtil.getOfflinePlayerIfEverPlayed("steve"))
                      .isSameAs(steve);
            Assertions.assertThat(BukkitUtil.getOfflinePlayerIfEverPlayed("Steve"))
                      .isSameAs(steve);
            Assertions.assertThat(BukkitUtil.getOfflinePlayerIfEverPlayed("Alex"))
                      .isNull();
        }
    }

    @Test
    void getMinecraftVersion_accepts_versions_with_suffixes() {
        try (MockedStatic<Bukkit> mockedStatic = Mockito.mockStatic(Bukkit.class)) {
            mockedStatic.when(Bukkit::getBukkitVersion)
                        .thenReturn("1.21.4-R0.1-SNAPSHOT");

            Assertions.assertThat(BukkitUtil.getMinecraftVersion())
                      .isEqualTo("1.21.4");
        }
    }
}
