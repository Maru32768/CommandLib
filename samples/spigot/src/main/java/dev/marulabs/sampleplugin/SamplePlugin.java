package dev.marulabs.sampleplugin;

import dev.marulabs.commandlib.CommandLib;
import org.bukkit.plugin.java.JavaPlugin;

public final class SamplePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        CommandLib.register(this,
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
