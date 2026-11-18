package dev.marulabs.samplefabric;

import dev.marulabs.cadence.Cadence;
import dev.marulabs.cadence.CadenceDiagnosticsCommand;
import net.fabricmc.api.ModInitializer;

public final class SampleFabricMod implements ModInitializer {
    @Override
    public void onInitialize() {
        Cadence.register("samplefabric.command",
                         new PingCommand(),
                         new IncrementCommand(),
                         new MathCommand(),
                         new ResetCommand(),
                         // Reports the platform and this mod's registrations. The name is up to the mod.
                         new CadenceDiagnosticsCommand("samplefabric-diagnostics"));
    }
}
