package dev.marulabs.cadence;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CadenceDiagnosticsCommandTest {
    @Test
    void sends_the_platform_and_the_registrations() {
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(new CadenceDiagnosticsCommand("diag"), "test.command")) {
            tester.execute("diag", sender);
        }

        assertThat(sender.getSentMessageTexts()).startsWith("Cadence diagnostics", "Platform:", "  Artifact: velocity")
                                                .contains("  Command registration: Velocity's CommandManager (one BrigadierCommand per label)");
    }
}
