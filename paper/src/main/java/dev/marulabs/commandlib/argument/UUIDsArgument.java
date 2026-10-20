package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.internal.StringUtil;
import dev.marulabs.commandlib.util.bukkit.BukkitUtil;
import dev.marulabs.commandlib.util.text.RichText;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class UUIDsArgument extends Argument<List<UUID>, UUIDsArgument> {
    public UUIDsArgument(String name) {
        super(name, ProfileWordArgumentType.profileWord());

        setSuggestionProvider(sb -> {
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
                                                            .toString(),
                                                           input);
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
                throw new ArgumentParseException(x -> x.sendFailure(RichText.translatable("argument.entity.notfound.player")));
            }
            if (s.equals("@r")) {
                Collections.shuffle(uuids, ThreadLocalRandom.current());
                return List.of(uuids.stream()
                                    .findFirst()
                                    .orElseThrow(() -> new ArgumentParseException(x -> x.sendFailure(RichText.translatable("argument.entity.notfound.player")))));
            }
            throw new ArgumentParseException(Caption.INVALID_SELECTOR, Map.of("value", s));
        }

        OfflinePlayer p = BukkitUtil.getOfflinePlayerIfEverPlayed(s);
        if (p != null) {
            return List.of(p.getUniqueId());
        }

        try {
            return List.of(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            throw new ArgumentParseException(Caption.UNKNOWN_PLAYER_OR_UUID, Map.of("value", s));
        }
    }
}
