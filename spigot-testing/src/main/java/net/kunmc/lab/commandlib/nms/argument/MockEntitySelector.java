package net.kunmc.lab.commandlib.nms.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import net.kunmc.lab.commandlib.CommandTester;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resolves entity arguments against the fake entities registered on {@link CommandTester}.
 *
 * <p>Supported input: a registered name, {@code @s} (the sender when it is an entity), {@code @p} and {@code @r}
 * (the first registered player), {@code @a} (all registered players) and {@code @e} (all registered entities).
 * Selector arguments such as {@code @e[type=zombie]} are not supported. Selectors matching nothing send the messages a
 * server sends, while a name that was never registered throws {@link IllegalArgumentException}.
 */
final class MockEntitySelector {
    static final String NO_ENTITY = "No entity was found";
    static final String NO_PLAYER = "No player was found";
    static final String TOO_MANY_ENTITIES = "Only one entity is allowed, but the provided selector allows more than one";
    static final String TOO_MANY_PLAYERS = "Only one player is allowed, but the provided selector allows more than one";
    static final String PLAYERS_ONLY = "Only players may be affected by this command, but the provided selector includes entities";

    private MockEntitySelector() {
    }

    /**
     * Parses a name or selector token, rejecting selectors that cannot match the argument while parsing.
     */
    static ArgumentType<String> type(boolean single, boolean playersOnly) {
        return reader -> {
            int start = reader.getCursor();
            String input = MockArgumentTypes.token()
                                            .parse(reader);
            if (input.startsWith("@") && input.contains("[")) {
                throw new UnsupportedOperationException("Selector arguments are not supported by the test mock: " + input);
            }
            boolean multiple = input.equals("@a") || input.equals("@e");
            if (single && multiple) {
                reader.setCursor(start);
                throw MockArgumentTypes.syntaxError(playersOnly ? TOO_MANY_PLAYERS : TOO_MANY_ENTITIES, reader);
            }
            if (playersOnly && input.equals("@e")) {
                reader.setCursor(start);
                throw MockArgumentTypes.syntaxError(PLAYERS_ONLY, reader);
            }
            return input;
        };
    }

    static List<Entity> select(String input) {
        List<Entity> registered = new ArrayList<>(CommandTester.getFakeEntities());
        switch (input) {
            case "@s":
                CommandSender sender = CommandTester.getCurrentCommandSender();
                return sender instanceof Entity ? List.of((Entity) sender) : List.of();
            case "@p":
            case "@r":
                return players(registered).stream()
                                          .map(Entity.class::cast)
                                          .limit(1)
                                          .collect(Collectors.toList());
            case "@a":
                return new ArrayList<>(players(registered));
            case "@e":
                return registered;
            default:
                // A server would report an unknown name as "No entity was found", but in a test it almost always means
                // a missing fixture, so fail loudly with a hint instead of silently not running the executor.
                Entity entity = CommandTester.getFakeEntity(input);
                if (entity == null) {
                    throw new IllegalArgumentException("No fake entity registered with name: " + input + ". Call withFakePlayer() or withFakeEntity() before execute().");
                }
                return List.of(entity);
        }
    }

    static Entity selectOne(String input) {
        List<Entity> entities = select(input);
        if (entities.isEmpty()) {
            throw MockArgumentTypes.resolveError(NO_ENTITY);
        }
        return entities.get(0);
    }

    static List<Entity> selectMany(String input) {
        List<Entity> entities = select(input);
        if (entities.isEmpty()) {
            throw MockArgumentTypes.resolveError(NO_ENTITY);
        }
        return entities;
    }

    static Player selectOnePlayer(String input) {
        List<Player> players = players(select(input));
        if (players.isEmpty()) {
            throw MockArgumentTypes.resolveError(NO_PLAYER);
        }
        return players.get(0);
    }

    static List<Player> selectManyPlayers(String input) {
        List<Player> players = players(select(input));
        if (players.isEmpty()) {
            throw MockArgumentTypes.resolveError(NO_PLAYER);
        }
        return players;
    }

    private static List<Player> players(List<Entity> entities) {
        return entities.stream()
                       .filter(Player.class::isInstance)
                       .map(Player.class::cast)
                       .collect(Collectors.toList());
    }
}
