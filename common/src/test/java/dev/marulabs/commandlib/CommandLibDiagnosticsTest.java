package dev.marulabs.commandlib;

import dev.marulabs.commandlib.CommandLibDiagnostics.Registration;
import dev.marulabs.commandlib.argument.CommonStringArgument;
import org.junit.jupiter.api.Test;

import java.util.List;

class CommandLibDiagnosticsTest {
    @Test
    void report_matches_snapshot() {
        TestCommand spawn = new TestCommand("spawn") {{
            addAliases("sp");
            execute(ctx -> {
            });
            argument(new CommonStringArgument<>("target")).execute((target, ctx) -> {
            });
        }};
        Registration live = new Registration("MyPlugin",
                                             "vanilla dispatcher",
                                             List.of("spawn", "sp", "myplugin:spawn"),
                                             CommandDocs.of("myplugin.command", spawn));
        Registration pending = new Registration("Other",
                                                "lifecycle handler",
                                                List.of(),
                                                CommandDocs.of("other.command", List.of()));
        CommandLibDiagnostics diagnostics = new CommandLibDiagnostics(List.of("Artifact: test", "Minecraft: 1.21.4"),
                                                                      List.of(live, pending));

        Snapshots.assertMatches("command-lib-diagnostics.snap", diagnostics.report());
    }
}
