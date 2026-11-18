package dev.marulabs.samplemod;

import dev.marulabs.cadence.Cadence;
import dev.marulabs.cadence.CadenceDiagnosticsCommand;
import net.minecraftforge.fml.common.Mod;

@Mod("samplemod")
public class SampleMod {
    public SampleMod() {
        Cadence.register("samplemod.command",
                         new PingCommand(),
                         new HelloCommand(),
                         new ConfigCommand(),
                         new PermissionCommand(),
                         new SuggestCommand(),
                         new OptionCommand(),
                         new PreprocessCommand(),
                         new IncrementCommand(),
                         new MathCommand(),
                         new ResetCommand(),
                         // Reports the platform and this mod's registrations. The name is up to the mod.
                         new CadenceDiagnosticsCommand("samplemod-diagnostics"));
    }
}
