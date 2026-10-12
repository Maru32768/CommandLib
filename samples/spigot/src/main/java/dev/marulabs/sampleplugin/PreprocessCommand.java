package dev.marulabs.sampleplugin;

import dev.marulabs.commandlib.Command;
import org.bukkit.entity.Player;

public class PreprocessCommand extends Command {
    public PreprocessCommand() {
        super("preprocesscommand");

        addPreprocess(ctx -> {
            if (ctx.getSender() instanceof Player) {
                return true;
            }
            ctx.sendFailure("player only command");
            return false;
        });

        execute(ctx -> {
            ctx.sendSuccess("success");
        });

        addChildren(new Command("sub") {{
            disableParentPreprocess();
            execute(ctx -> ctx.sendSuccess("always success"));
        }});
    }
}
