package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import net.kyori.adventure.text.Component;

import java.util.List;

final class SelectorResults {
    static final String NO_PLAYER = "argument.entity.notfound.player";
    static final String NO_ENTITY = "argument.entity.notfound.entity";

    private SelectorResults() {
    }

    /**
     * Rejects a selector that matched nothing with vanilla's error, as {@code EntityArgument.getPlayers} and
     * {@code getEntities} do. Paper's {@code players()} and {@code entities()} resolvers return an empty list instead.
     */
    static <T> List<T> requireAny(List<T> resolved, String translationKey) throws ArgumentParseException {
        if (resolved.isEmpty()) {
            throw new ArgumentParseException(ctx -> ((CommandContext) ctx).sendFailure(Component.translatable(translationKey)));
        }
        return resolved;
    }
}
