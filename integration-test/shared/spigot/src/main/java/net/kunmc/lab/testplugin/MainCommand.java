package net.kunmc.lab.testplugin;

import net.kunmc.lab.integration.core.TestStatus;

import net.kunmc.lab.commandlib.Command;
import org.bukkit.entity.Player;

public class MainCommand extends Command {
    public static final String NAME = "commandlibtest";

    public MainCommand() {
        super(NAME);

        addPreprocess(ctx -> {
            if (ctx.getSender() instanceof Player) {
                ctx.sendSuccess(ctx.getArguments());
            }
        });
    }
}
