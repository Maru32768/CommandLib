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

        assertThat(sender.getSentMessageTexts()).startsWith("CommandLib diagnostics", "Platform:")
                                                .contains("  Java: " + System.getProperty("java.version"))
                                                .anyMatch(x -> x.startsWith("  Loader: "))
                                                .anyMatch(x -> x.startsWith("Registrations: "));
    }

    @Test
    void registration_returns_its_documentation() {
        String tree = CommandDocs.of("test.command", new CommandLibDiagnosticsCommand("diag"))
                                 .tree();

        assertThat(tree).startsWith("diag [test.command.diag, OP]");
    }
}
