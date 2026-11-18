package dev.marulabs.sampleplugin;

import dev.marulabs.cadence.Cadence;
import dev.marulabs.cadence.CadenceDiagnosticsCommand;
import dev.marulabs.cadence.annotation.AnnotatedCommands;
import org.bukkit.plugin.java.JavaPlugin;

public final class SamplePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        Cadence.register(this,
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
                         new CadenceDiagnosticsCommand("sample-diagnostics"));
    }
}
