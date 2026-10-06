package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver;

import java.util.Arrays;
import java.util.Collection;

/**
 * Reads a single token like {@link RawWordArgumentType}, but is advertised to clients as {@code minecraft:game_profile},
 * as on Spigot. Clients accept player names, UUIDs and selectors such as {@code @a} for it without treating it as
 * consuming the rest of the input, so later arguments keep their client-side completion.
 */
@SuppressWarnings("UnstableApiUsage")
class ProfileWordArgumentType implements CustomArgumentType<String, PlayerProfileListResolver> {
    private static final Collection<String> EXAMPLES = Arrays.asList("Player", "@a", "dd12be42-52a9-4a91-a8a1-11c01849e498");

    static ProfileWordArgumentType profileWord() {
        return new ProfileWordArgumentType();
    }

    @Override
    public String parse(StringReader reader) {
        int start = reader.getCursor();
        while (reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }
        return reader.getString()
                     .substring(start, reader.getCursor());
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    @Override
    public ArgumentType<PlayerProfileListResolver> getNativeType() {
        return ArgumentTypes.playerProfiles();
    }
}
