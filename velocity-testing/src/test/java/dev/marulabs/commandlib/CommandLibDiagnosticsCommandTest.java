package dev.marulabs.commandlib;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandLibDiagnosticsCommandTest {
    @Test
    void sends_the_platform_and_the_registrations() {
        FakeSender sender = FakeSender.console();

        try (CommandTester tester = new CommandTester(new CommandLibDiagnosticsCommand("diag"), "test.command")) {
            tester.execute("diag", sender);
        }

        assertThat(sender.getSentMessageTexts()).startsWith("CommandLib diagnostics",
                                                            "Platform:",
                                                            "  Artifact: velocity")
                                                .contains("  Command registration: Velocity's CommandManager (one BrigadierCommand per label)");
    }
}
