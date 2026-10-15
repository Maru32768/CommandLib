package dev.marulabs.samplemod;

import dev.marulabs.commandlib.CommandLib;
import dev.marulabs.commandlib.CommandLibDiagnosticsCommand;
import net.minecraftforge.fml.common.Mod;

@Mod("samplemod")
public class SampleMod {
    public SampleMod() {
        CommandLib.register("samplemod.command",
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
                            new CommandLibDiagnosticsCommand("samplemod-diagnostics"));
    }
}
