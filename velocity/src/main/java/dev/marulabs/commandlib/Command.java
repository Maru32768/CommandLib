package dev.marulabs.commandlib;

import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import org.jetbrains.annotations.NotNull;

public class Command extends CommonCommand<CommandContext, Command> {
    public Command(@NotNull String name) {
        super(name);
    }

    public static Extractor<CommandContext, Player> playerRequirer() {
        return ctx -> {
            if (!(ctx.getSender() instanceof Player)) {
                throw new CommandPrerequisiteException("This command can only be executed by a player.");
            }
            return (Player) ctx.getSender();
        };
    }

    public static Extractor<CommandContext, ConsoleCommandSource> consoleRequirer() {
        return ctx -> {
            if (!(ctx.getSender() instanceof ConsoleCommandSource)) {
                throw new CommandPrerequisiteException("This command can only be executed from the console.");
            }
            return (ConsoleCommandSource) ctx.getSender();
        };
    }

    public final void requirePlayer() {
        addPrerequisite(playerRequirer()::extract);
    }

    public final void requireConsole() {
        addPrerequisite(consoleRequirer()::extract);
    }
}
