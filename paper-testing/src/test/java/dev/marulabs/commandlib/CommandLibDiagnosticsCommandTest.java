package dev.marulabs.commandlib;

import org.bukkit.Bukkit;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class CommandLibDiagnosticsCommandTest {
    @Test
    void sends_the_platform_and_the_registrations() {
        FakeSender sender = FakeSender.console();

        try (MockedStatic<Bukkit> bukkit = mockServer();
             CommandTester tester = new CommandTester(new CommandLibDiagnosticsCommand("diag"), "test.command")) {
            tester.execute("diag", sender);
        }

        assertThat(sender.getSentMessageTexts()).startsWith("CommandLib diagnostics", "Platform:", "  Artifact: paper")
                                                .contains("  Server: Paper 1.21.4-232-main (MC: 1.21.4)",
                                                          "  Minecraft: 1.21.4",
                                                          "  Command registration: Paper's Commands API (LifecycleEvents.COMMANDS)");
    }

    private static MockedStatic<Bukkit> mockServer() {
        MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getName)
              .thenReturn("Paper");
        bukkit.when(Bukkit::getVersion)
              .thenReturn("1.21.4-232-main (MC: 1.21.4)");
        bukkit.when(Bukkit::getMinecraftVersion)
              .thenReturn("1.21.4");
        return bukkit;
    }
}
