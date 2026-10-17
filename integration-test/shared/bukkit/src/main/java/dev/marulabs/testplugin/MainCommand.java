package dev.marulabs.testplugin;

import dev.marulabs.commandlib.Command;
import dev.marulabs.integration.core.TestStatus;
import org.bukkit.entity.Player;

public class MainCommand extends Command {
    public static final String NAME = "commandlibtest";

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
