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

        assertThat(sender.getSentMessageTexts()).startsWith("CommandLib diagnostics", "Platform:", "  Artifact: spigot")
                                                .contains("  Server: CraftBukkit git-Spigot (MC: 1.13.2)",
                                                          "  Minecraft: 1.13.2",
                                                          "  Command dispatcher: vanilla Brigadier dispatcher through NMS",
                                                          "  Typed NMS module: none supports Minecraft 1.13.2",
                                                          "Registrations: 0");
    }

    @Test
    void sections_say_when_nothing_is_registered() {
        FakeSender sender = FakeSender.console();

        try (MockedStatic<Bukkit> bukkit = mockServer();
             CommandTester tester = new CommandTester(new CommandLibDiagnosticsCommand("diag"), "test.command")) {
            tester.execute("diag commands", sender);
            tester.execute("diag permissions", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("No registrations.", "No registrations.");
    }

    @Test
    void its_permissions_follow_the_registration_prefix() {
        String tree = CommandDocs.of("test.command", new CommandLibDiagnosticsCommand("diag"))
                                 .tree();

        assertThat(tree).isEqualTo("diag [test.command.diag, OP]\n" + "  commands [test.command.diag.commands, OP]\n"
                + "  permissions [test.command.diag.permissions, OP]\n");
    }

    private static MockedStatic<Bukkit> mockServer() {
        MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getName)
              .thenReturn("CraftBukkit");
        bukkit.when(Bukkit::getVersion)
              .thenReturn("git-Spigot (MC: 1.13.2)");
        bukkit.when(Bukkit::getBukkitVersion)
              .thenReturn("1.13.2-R0.1-SNAPSHOT");
        bukkit.when(Bukkit::isPrimaryThread)
              .thenReturn(true);
        return bukkit;
    }
}
