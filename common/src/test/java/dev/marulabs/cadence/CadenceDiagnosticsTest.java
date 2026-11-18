package dev.marulabs.cadence;

import dev.marulabs.cadence.CadenceDiagnostics.Registration;
import dev.marulabs.cadence.argument.CommonStringArgument;
import org.junit.jupiter.api.Test;

import java.util.List;

class CadenceDiagnosticsTest {
    @Test
    void report_matches_snapshot() {
        TestCommand spawn = new TestCommand("spawn") {{
            addAliases("sp");
            execute(ctx -> {
            });
            argument(new CommonStringArgument<>("target")).execute((ctx, target) -> {
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
        CadenceDiagnostics diagnostics = new CadenceDiagnostics(List.of("Artifact: test", "Minecraft: 1.21.4"),
                                                                List.of(live, pending));

        Snapshots.assertMatches("cadence-diagnostics.snap", diagnostics.report());
    }
}
