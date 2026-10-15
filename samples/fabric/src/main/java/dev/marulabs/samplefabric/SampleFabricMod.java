package dev.marulabs.samplefabric;

import dev.marulabs.commandlib.CommandLib;
import dev.marulabs.commandlib.CommandLibDiagnosticsCommand;
import net.fabricmc.api.ModInitializer;

public final class SampleFabricMod implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandLib.register("samplefabric.command",
                            new PingCommand(),
                            new IncrementCommand(),
                            new MathCommand(),
                            new ResetCommand(),
                            // Reports the platform and this mod's registrations. The name is up to the mod.
                            new CommandLibDiagnosticsCommand("samplefabric-diagnostics"));
    }
}
