package net.kunmc.lab.commandlib.argument;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.kunmc.lab.commandlib.util.StringUtil;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.players.GameProfileCache;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class UUIDsArgument extends Argument<List<UUID>, UUIDsArgument> {
    public UUIDsArgument(String name) {
        super(name, GameProfileArgument.gameProfile());

        setSuggestionAction(sb -> {
            String input = sb.getLatestInput();

            Map<UUID, String> uuidToNameMap = new HashMap<>();
            sb.getContext()
              .getHandle()
              .getSource()
              .getOnlinePlayerNames()
              .stream()
              .map(getProfileCache()::get)
              .flatMap(Optional::stream)
              .filter(x -> filter(sb.getContext()).test(List.of(x.getId())))
              .filter(x -> {
                  if (input.isEmpty()) {
                      return true;
                  }

                  if (x.getName() != null && StringUtil.containsIgnoreCase(x.getName(), input)) {
                      return true;
                  }
                  return StringUtil.containsIgnoreCase(x.getId()
                                                        .toString(), input);
              })
              .forEach(x -> uuidToNameMap.put(x.getId(), x.getName()));

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
        return ((List<UUID>) parsedArgument);
    }

    @Override
    protected List<UUID> parseImpl(CommandContext ctx) throws CommandSyntaxException, ArgumentParseException {
        String s = ctx.getInput(name());

        if (s.startsWith("@")) {
            List<UUID> uuids = ctx.getHandle()
                                  .getSource()
                                  .getOnlinePlayerNames()
                                  .stream()
                                  .map(getProfileCache()::get)
              .flatMap(Optional::stream)
                                  .map(GameProfile::getId)
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

        Optional<GameProfile> gameProfile = getProfileCache().get(s);
        if (gameProfile.isPresent()) {
            return List.of(gameProfile.get().getId());
        }

        try {
            return List.of(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            throw new ArgumentParseException(x -> x.sendFailure(s + " is not found or not valid UUID"));
        }
    }

    private static GameProfileCache getProfileCache() {
        return ServerLifecycleHooks.getCurrentServer()
                                   .getProfileCache();
    }
}
