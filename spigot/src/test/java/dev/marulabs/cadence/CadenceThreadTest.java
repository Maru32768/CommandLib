package dev.marulabs.cadence;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.objenesis.ObjenesisStd;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CadenceThreadTest {
    private final PluginManager pluginManager = mock(PluginManager.class);
    private final Plugin plugin = mock(Plugin.class);
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        when(plugin.getName()).thenReturn("Test");
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager)
              .thenReturn(pluginManager);
        bukkit.when(Bukkit::isPrimaryThread)
              .thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void registering_off_the_main_thread_fails_before_adding_anything() {
        assertThatThrownBy(() -> Cadence.register(plugin,
                                                  new Command("spawn"))).isInstanceOf(IllegalStateException.class)
                                                                        .hasMessage("Commands must be registered on the main server thread");

        verifyNoInteractions(pluginManager);
    }

    @Test
    void unregistering_off_the_main_thread_fails() {
        Cadence unregistered = new ObjenesisStd().newInstance(Cadence.class);

        assertThatThrownBy(unregistered::unregister).isInstanceOf(IllegalStateException.class)
                                                    .hasMessage("Commands must be unregistered on the main server thread");
    }

    @Test
    void unregistering_again_does_nothing() {
        bukkit.when(Bukkit::isPrimaryThread)
              .thenReturn(true);
        // An instance created without its constructor is not registered, as one is after its first unregister().
        Cadence unregistered = new ObjenesisStd().newInstance(Cadence.class);

        unregistered.unregister();

        verifyNoInteractions(pluginManager);
        bukkit.verify(Bukkit::getOnlinePlayers, never());
    }
}
