package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.StringUtil;
import dev.marulabs.commandlib.util.bukkit.BukkitUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Arrays;
import java.util.Objects;

public class OfflinePlayerArgument extends Argument<OfflinePlayer, OfflinePlayerArgument> {
    public OfflinePlayerArgument(String name) {
        super(name, StringArgumentType.word());

        addSuggestionAction(sb -> {
            Arrays.stream(Bukkit.getOfflinePlayers())
                  .filter(filter(sb.getContext()))
                  .map(OfflinePlayer::getName)
                  .filter(Objects::nonNull)
                  .filter(x -> sb.getLatestInput()
                                 .isEmpty()
                          || StringUtil.containsIgnoreCase(x, sb.getLatestInput()))
                  .forEach(sb::suggest);
        });
    }

    @Override
    public OfflinePlayer cast(Object parsedArgument) {
        return (OfflinePlayer) parsedArgument;
    }

    @Override
    protected OfflinePlayer parseImpl(CommandContext ctx) throws ArgumentParseException {
        String s = StringArgumentType.getString(ctx.getHandle(), name());
        OfflinePlayer player = BukkitUtil.getOfflinePlayerIfEverPlayed(s);
        if (player == null) {
            throw ArgumentParseException.ofIncorrectInput(this.name(), ctx, s);
        }
        return player;
    }
}
