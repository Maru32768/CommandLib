package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.kunmc.lab.commandlib.util.StringUtil;
import net.kunmc.lab.commandlib.util.bukkit.BukkitUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class OfflinePlayersArgument extends Argument<List<OfflinePlayer>, OfflinePlayersArgument> {
    public OfflinePlayersArgument(String name) {
        super(name, RawWordArgumentType.rawWord());

        addSuggestionAction(sb -> {
            String input = sb.getLatestInput();

            Arrays.stream(Bukkit.getOfflinePlayers())
                  .filter(x -> filter(sb.getContext()).test(List.of(x)))
                  .filter(x -> {
                      if (input.isEmpty()) {
                          return true;
                      }
                      return x.getName() != null && StringUtil.containsIgnoreCase(x.getName(), input);
                  })
                  .map(OfflinePlayer::getName)
                  .filter(Objects::nonNull)
                  .forEach(sb::suggest);

            List.of("@a", "@r")
                .stream()
                .filter(x -> input.isEmpty() || x.startsWith(input))
                .forEach(sb::suggest);
        });
    }

    @Override
    public List<OfflinePlayer> cast(Object parsedArgument) {
        return (List<OfflinePlayer>) parsedArgument;
    }

    @Override
    protected List<OfflinePlayer> parseImpl(CommandContext ctx) throws ArgumentParseException {
        String s = ctx.getInput(name());

        if (s.startsWith("@")) {
            List<OfflinePlayer> players = Arrays.stream(Bukkit.getOfflinePlayers())
                                                .collect(Collectors.toList());
            if (s.equals("@a")) {
                if (!players.isEmpty()) {
                    return players;
                }
                throw new ArgumentParseException(x -> x.sendFailure("no player found."));
            }
            if (s.equals("@r")) {
                Collections.shuffle(players, ThreadLocalRandom.current());
                return List.of(players.stream()
                                      .findFirst()
                                      .orElseThrow(() -> new ArgumentParseException(x -> x.sendFailure(
                                              "no player found."))));
            }
            throw new ArgumentParseException(x -> x.sendFailure(s + " is invalid selector."));
        }

        OfflinePlayer p = BukkitUtil.getOfflinePlayerIfEverPlayed(s);
        if (p == null) {
            throw ArgumentParseException.ofIncorrectInput(this.name(), ctx, s);
        }
        return List.of(p);
    }
}
