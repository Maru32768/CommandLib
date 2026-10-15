package dev.marulabs.commandlib;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CommandTreeRefreshTest {
    private final Player alice = mock(Player.class);
    private final Player bob = mock(Player.class);
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getOnlinePlayers)
              .thenReturn(List.of(alice, bob));
        bukkit.when(Bukkit::isPrimaryThread)
              .thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void refresh_sends_the_tree_to_the_player_only() {
        CommandLib.refresh(alice);

        verify(alice).updateCommands();
        verify(bob, never()).updateCommands();
    }

    @Test
    void refreshAll_sends_the_tree_to_every_online_player() {
        CommandLib.refreshAll();

        verify(alice).updateCommands();
        verify(bob).updateCommands();
    }

    @Test
    void refreshing_off_the_main_thread_fails_without_sending_the_tree() {
        bukkit.when(Bukkit::isPrimaryThread)
              .thenReturn(false);

        assertThatThrownBy(() -> CommandLib.refresh(alice)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(CommandLib::refreshAll).isInstanceOf(IllegalStateException.class);
        verify(alice, never()).updateCommands();
        verify(bob, never()).updateCommands();
    }
}
