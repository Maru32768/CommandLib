package dev.marulabs.sampleplugin;

import dev.marulabs.commandlib.CommandLib;
import dev.marulabs.commandlib.CommandLibDiagnosticsCommand;
import dev.marulabs.commandlib.annotation.AnnotatedCommands;
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
                            new MathCommand(),
                            new ResetCommand(),
                            new LookupCommand(getLogger()),
                            AnnotatedCommands.parse(new ArenaCommand()),
                            FeedCommand.create(),
                            // Reports the platform and this plugin's registrations. The name is up to the plugin.
                            new CommandLibDiagnosticsCommand("sample-diagnostics"));
    }
}
