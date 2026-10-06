package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.kunmc.lab.commandlib.util.StringUtil;
import net.kunmc.lab.commandlib.util.bukkit.BukkitUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UUIDArgument extends Argument<UUID, UUIDArgument> {
    public UUIDArgument(String name) {
        super(name, StringArgumentType.word());

        setSuggestionAction(sb -> {
            Map<UUID, String> uuidToNameMap = new HashMap<>();
            Arrays.stream(Bukkit.getOfflinePlayers())
                  .filter(x -> filter(sb.getContext()).test(x.getUniqueId()))
                  .filter(x -> {
                      String input = sb.getLatestInput();
                      if (input.isEmpty()) {
                          return true;
                      }
                      if (x.getName() != null && StringUtil.containsIgnoreCase(x.getName(), input)) {
                          return true;
                      }
                      return StringUtil.containsIgnoreCase(x.getUniqueId()
                                                            .toString(), input);
                  })
                  .forEach(x -> uuidToNameMap.put(x.getUniqueId(), x.getName()));
            uuidToNameMap.forEach((k, v) -> {
                if (v == null) {
                    sb.suggest(k.toString());
                } else {
                    sb.suggest(v, k.toString());
                }
            });
        });
    }

    @Override
    public UUID cast(Object parsedArgument) {
        return (UUID) parsedArgument;
    }

    @Override
    protected UUID parseImpl(CommandContext ctx) throws ArgumentParseException {
        String s = StringArgumentType.getString(ctx.getHandle(), name());

        OfflinePlayer p = BukkitUtil.getOfflinePlayerIfEverPlayed(s);
        if (p != null) {
            return p.getUniqueId();
        }

        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            throw new ArgumentParseException(x -> x.sendFailure(s + " is not found or not valid UUID"));
        }
    }
}
