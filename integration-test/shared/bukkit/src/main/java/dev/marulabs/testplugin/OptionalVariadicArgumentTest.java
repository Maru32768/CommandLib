package dev.marulabs.testplugin;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;
import dev.marulabs.commandlib.argument.StringArgument;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Trailing optional and variadic arguments, dispatched from the console like the other cases.
 */
public final class OptionalVariadicArgumentTest extends TestBase {
    private final String playerName;

    public OptionalVariadicArgumentTest(Command command, String playerName) {
        super(command);
        this.playerName = playerName;
    }

    @Override
    public List<String> build() {
        List<String> commands = new ArrayList<>();

        commands.addAll(optionalOmitted());
        commands.addAll(optionalGiven());
        commands.addAll(optionalExtractorOmitted());
        commands.addAll(variadicValues());
        commands.addAll(variadicPlayers());
        commands.addAll(optionalVariadicOmitted());

        return commands;
    }

    public List<String> optionalOmitted() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new IntegerArgument("a"), new IntegerArgument("b").optional(7)).execute((ctx, a, b) -> {
                putResult(key, a + "," + b, "1,7");
            });
        }});

        return List.of(buildCommand(command, name + " 1"));
    }

    public List<String> optionalGiven() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new IntegerArgument("a"), new IntegerArgument("b").optional(7)).execute((ctx, a, b) -> {
                putResult(key, a + "," + b, "1,3");
            });
        }});

        return List.of(buildCommand(command, name + " 1 3"));
    }

    // The extractor runs per execution with the context, so the default is the console's name.
    public List<String> optionalExtractorOmitted() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new StringArgument("a").optional(ctx -> ctx.getSender()
                                                                .getName())).execute((ctx, a) -> {
                                                                    putResult(key, a, "CONSOLE");
                                                                });
        }});

        return List.of(buildCommand(command, name));
    }

    public List<String> variadicValues() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new StringArgument("a"), new IntegerArgument("b").variadic(3)).execute((ctx, a, b) -> {
                putResult(key, a + ":" + b, "x:[3, 1, 2]");
            });
        }});

        return List.of(buildCommand(command, name + " x 3 1 2"));
    }

    public List<String> variadicPlayers() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new PlayerArgument("a").variadic()).execute((ctx, a) -> {
                putResult(key,
                          a.stream()
                           .map(Player::getName)
                           .collect(Collectors.joining(",")),
                          playerName + "," + playerName);
            });
        }});

        return List.of(buildCommand(command, name + " " + playerName + " @p"));
    }

    public List<String> optionalVariadicOmitted() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new IntegerArgument("a").variadic()
                                             .optional(List.of())).execute((ctx, a) -> {
                                                 putResult(key, String.valueOf(a), "[]");
                                             });
        }});

        return List.of(buildCommand(command, name));
    }
}
