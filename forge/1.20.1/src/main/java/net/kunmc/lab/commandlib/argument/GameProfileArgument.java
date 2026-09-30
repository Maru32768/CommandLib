package net.kunmc.lab.commandlib.argument;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.kunmc.lab.commandlib.util.StringUtil;
import net.minecraft.server.players.GameProfileCache;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Objects;
import java.util.Optional;

public class GameProfileArgument extends Argument<GameProfile, GameProfileArgument> {
    public GameProfileArgument(String name) {
        super(name, StringArgumentType.string());

        addSuggestionAction(sb -> {
            sb.getContext()
              .getHandle()
              .getSource()
              .getOnlinePlayerNames()
              .stream()
              .map(getProfileCache()::get)
              .flatMap(Optional::stream)
              .filter(x -> Objects.nonNull(x.getName()))
              .filter(filter(sb.getContext()))
              .map(GameProfile::getName)
              .filter(x -> sb.getLatestInput()
                             .isEmpty() || StringUtil.containsIgnoreCase(x, sb.getLatestInput()))
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
                  .flatMap(Optional::stream)
                  .filter(x -> x.getName()
                                .equalsIgnoreCase(s))
                  .findFirst()
                  .orElseThrow(() -> ArgumentParseException.ofIncorrectInput(this.name(), ctx, s));
    }

    private static GameProfileCache getProfileCache() {
        return ServerLifecycleHooks.getCurrentServer()
                                   .getProfileCache();
    }
}
