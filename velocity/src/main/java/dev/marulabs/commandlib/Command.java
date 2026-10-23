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

    /**
     * A builder of a command named {@code name}, which configures it in one chain. See {@link CommandBuilder}.
     */
    @NotNull
    public static CommandBuilder<CommandContext, Command> builder(@NotNull String name) {
        return CommandBuilder.of(new Command(name));
    }

    public static Extractor<CommandContext, Player> player() {
        return ctx -> {
            if (!(ctx.getSender() instanceof Player)) {
                throw new CommandPrerequisiteException(Caption.REQUIRE_PLAYER);
            }
            return (Player) ctx.getSender();
        };
    }

    public static Extractor<CommandContext, ConsoleCommandSource> console() {
        return ctx -> {
            if (!(ctx.getSender() instanceof ConsoleCommandSource)) {
                throw new CommandPrerequisiteException(Caption.REQUIRE_CONSOLE);
            }
            return (ConsoleCommandSource) ctx.getSender();
        };
    }
}
