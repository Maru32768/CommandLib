package dev.marulabs.testplugin;

import dev.marulabs.cadence.Command;
import dev.marulabs.integration.core.TestStatus;
import org.bukkit.entity.Player;

public class MainCommand extends Command {
    public static final String NAME = "cadencetest";

    public MainCommand() {
        super(NAME);

        addPreprocess(ctx -> {
            if (ctx.getSender() instanceof Player) {
                ctx.sendSuccess(ctx.getArguments());
            }
            return true;
        });
    }
}
