package net.kunmc.lab.commandlib.argument;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.kunmc.lab.commandlib.util.StringUtil;
import net.minecraft.server.players.GameProfileCache;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class UUIDArgument extends Argument<UUID, UUIDArgument> {
    public UUIDArgument(String name) {
        super(name, StringArgumentType.string());

        setSuggestionAction(sb -> {
            Map<UUID, String> uuidToNameMap = new HashMap<>();
            sb.getContext()
              .getHandle()
              .getSource()
              .getOnlinePlayerNames()
              .stream()
              .map(getProfileCache()::get)
              .flatMap(Optional::stream)
              .filter(x -> filter(sb.getContext()).test(x.getId()))
              .filter(x -> {
                  String input = sb.getLatestInput();
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
        });
    }

    @Override
    public UUID cast(Object parsedArgument) {
        return ((UUID) parsedArgument);
    }

    @Override
    protected UUID parseImpl(CommandContext ctx) throws CommandSyntaxException, ArgumentParseException {
        String s = StringArgumentType.getString(ctx.getHandle(), name());

        Optional<GameProfile> gameProfile = getProfileCache().get(s);
        if (gameProfile.isPresent()) {
            return gameProfile.get().getId();
        }

        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            throw new ArgumentParseException(x -> x.sendFailure(s + " is not found or not valid UUID"));
        }
    }

    private static GameProfileCache getProfileCache() {
        return ServerLifecycleHooks.getCurrentServer()
                                   .getProfileCache();
    }
}
