package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.kunmc.lab.commandlib.util.StringUtil;
import net.kunmc.lab.commandlib.util.bukkit.BukkitUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class UUIDsArgument extends Argument<List<UUID>, UUIDsArgument> {
    public UUIDsArgument(String name) {
        super(name, RawWordArgumentType.rawWord());

        setSuggestionAction(sb -> {
            String input = sb.getLatestInput();

            Map<UUID, String> uuidToNameMap = new HashMap<>();
            Arrays.stream(Bukkit.getOfflinePlayers())
                  .filter(x -> filter(sb.getContext()).test(List.of(x.getUniqueId())))
                  .filter(x -> {
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

            List.of("@a", "@r")
                .stream()
                .filter(x -> input.isEmpty() || x.startsWith(input))
                .forEach(sb::suggest);
        });
    }

    @Override
    public List<UUID> cast(Object parsedArgument) {
        return (List<UUID>) parsedArgument;
    }

    @Override
    protected List<UUID> parseImpl(CommandContext ctx) throws ArgumentParseException {
        String s = ctx.getInput(name());

        if (s.startsWith("@")) {
            List<UUID> uuids = Arrays.stream(Bukkit.getOfflinePlayers())
                                     .map(OfflinePlayer::getUniqueId)
                                     .collect(Collectors.toList());
            if (s.equals("@a")) {
                if (!uuids.isEmpty()) {
                    return uuids;
                }
                throw new ArgumentParseException(x -> x.sendFailure("no player found"));
            }
            if (s.equals("@r")) {
                Collections.shuffle(uuids, ThreadLocalRandom.current());
                return List.of(uuids.stream()
                                    .findFirst()
                                    .orElseThrow(() -> new ArgumentParseException(x -> x.sendFailure("no player found"))));
            }
            throw new ArgumentParseException(x -> x.sendFailure(s + " is invalid selector"));
        }

        OfflinePlayer p = BukkitUtil.getOfflinePlayerIfEverPlayed(s);
        if (p != null) {
            return List.of(p.getUniqueId());
        }

        try {
            return List.of(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            throw new ArgumentParseException(x -> x.sendFailure(s + " is not found or not valid UUID"));
        }
    }
}
