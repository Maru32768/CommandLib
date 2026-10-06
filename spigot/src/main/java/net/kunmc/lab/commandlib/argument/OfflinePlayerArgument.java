package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.kunmc.lab.commandlib.util.StringUtil;
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
                                 .isEmpty() || StringUtil.containsIgnoreCase(x, sb.getLatestInput()))
                  .forEach(sb::suggest);
        });
    }

    @Override
    public OfflinePlayer cast(Object parsedArgument) {
        return ((OfflinePlayer) parsedArgument);
    }

    @Override
    protected OfflinePlayer parseImpl(CommandContext ctx) throws ArgumentParseException {
        String s = StringArgumentType.getString(ctx.getHandle(), name());
        return Arrays.stream(Bukkit.getOfflinePlayers())
                     .filter(x -> x.getName() != null && x.getName()
                                                          .equalsIgnoreCase(s))
                     .findFirst()
                     .orElseThrow(() -> ArgumentParseException.ofIncorrectInput(this.name(), ctx, s));
    }
}
