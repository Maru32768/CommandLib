package dev.marulabs.cadence.argument;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import dev.marulabs.cadence.internal.CurrentServer;
import dev.marulabs.cadence.internal.StringUtil;
import net.minecraft.server.players.GameProfileCache;

import java.util.Objects;
import java.util.Optional;

public class GameProfileArgument extends Argument<GameProfile, GameProfileArgument> {
    public GameProfileArgument(String name) {
        super(name, StringArgumentType.string());

        addSuggestionProvider(sb -> {
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
              .filter(x -> Objects.nonNull(x.getName()))
              .filter(filter(sb.getContext()))
              .map(GameProfile::getName)
              .filter(x -> sb.getLatestInput()
                             .isEmpty()
                      || StringUtil.containsIgnoreCase(x, sb.getLatestInput()))
              .forEach(sb::suggest);
        });
    }

    @Override
    public GameProfile cast(Object parsedArgument) {
        return ((GameProfile) parsedArgument);
    }

    @Override
    protected GameProfile parseImpl(CommandContext ctx) throws ArgumentParseException {
        String s = StringArgumentType.getString(ctx.getHandle(), name());
        return ctx.getHandle()
                  .getSource()
                  .getOnlinePlayerNames()
                  .stream()
                  .map(getProfileCache()::get)
                  //? if >=1.17 {
                  .flatMap(Optional::stream)
                  //?} else
                  /*.filter(Objects::nonNull)*/
                  .filter(x -> x.getName()
                                .equalsIgnoreCase(s))
                  .findFirst()
                  .orElseThrow(() -> ArgumentParseException.ofIncorrectInput(this.name(), ctx, s));
    }

    private static GameProfileCache getProfileCache() {
        return Objects.requireNonNull(CurrentServer.get(), "no server is running")
                      .getProfileCache();
    }
}
