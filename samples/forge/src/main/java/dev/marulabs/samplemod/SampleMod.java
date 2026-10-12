package dev.marulabs.samplemod;

import dev.marulabs.commandlib.CommandLib;
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
                            new MathCommand());
    }
}
