package dev.marulabs.commandlib.argument;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.CurrentServer;
import dev.marulabs.commandlib.util.StringUtil;
import net.minecraft.server.players.GameProfileCache;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class UUIDArgument extends Argument<UUID, UUIDArgument> {
    public UUIDArgument(String name) {
        super(name, StringArgumentType.string());

        setSuggestionProvider(sb -> {
            Map<UUID, String> uuidToNameMap = new HashMap<>();
            sb.getContext()
              .getHandle()
              .getSource()
              .getOnlinePlayerNames()
              .stream()
              .map(getProfileCache()::get)
              //? if >=1.17 {
              .flatMap(Optional::stream)
              //?} else
              /*.filter(Objects::nonNull)*/
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
                                                        .toString(),
                                                       input);
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

        //? if >=1.17 {
        Optional<GameProfile> gameProfile = getProfileCache().get(s);
        if (gameProfile.isPresent()) {
            return gameProfile.get()
                              .getId();
        }
        //?} else {
        /*GameProfile gameProfile = getProfileCache().get(s);
        if (gameProfile != null) {
            return gameProfile.getId();
        }
        *///?}

        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            throw new ArgumentParseException(x -> x.sendFailure(s + " is not found or not valid UUID"));
        }
    }

    private static GameProfileCache getProfileCache() {
        return Objects.requireNonNull(CurrentServer.get(), "no server is running")
                      .getProfileCache();
    }
}
