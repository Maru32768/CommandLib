package dev.marulabs.testmod;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;
import dev.marulabs.commandlib.argument.StringArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// Trailing optional and variadic arguments, dispatched from the server console source like the other cases.
public final class OptionalVariadicArgumentTest extends TestBase {
    private final String playerName;

    public OptionalVariadicArgumentTest(Command command, String playerName) {
        super(command);
        this.playerName = playerName;
    }

    @Override
    public List<String> build() {
        List<String> commands = new ArrayList<>();

        commands.add(optionalOmitted());
        commands.add(optionalGiven());
        commands.add(optionalExtractorOmitted());
        commands.add(variadicValues());
        commands.add(variadicPlayers());
        commands.add(optionalVariadicOmitted());

        return commands;
    }

    private String optionalOmitted() {
        String name = "optionalOmitted";
        String key = getKey(name);

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new IntegerArgument("a"), new IntegerArgument("b").optional(7)).execute((ctx, a, b) -> {
                putResult(key, a + "," + b, "1,7");
            });
        }});

        return buildCommand(name + " 1");
    }

    private String optionalGiven() {
        String name = "optionalGiven";
        String key = getKey(name);

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new IntegerArgument("a"), new IntegerArgument("b").optional(7)).execute((ctx, a, b) -> {
                putResult(key, a + "," + b, "1,3");
            });
        }});

        return buildCommand(name + " 1 3");
    }

    // The extractor runs per execution with the context, so the default is the console source's name.
    private String optionalExtractorOmitted() {
        String name = "optionalExtractorOmitted";
        String key = getKey(name);

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new StringArgument("a").optional(ctx -> ctx.getSender()
                                                                .getTextName())).execute((ctx, a) -> {
                                                                    putResult(key, a, "Server");
                                                                });
        }});

        return buildCommand(name);
    }

    private String variadicValues() {
        String name = "variadicValues";
        String key = getKey(name);

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new StringArgument("a"), new IntegerArgument("b").variadic(3)).execute((ctx, a, b) -> {
                putResult(key, a + ":" + b, "x:[3, 1, 2]");
            });
        }});

        return buildCommand(name + " x 3 1 2");
    }

    private String variadicPlayers() {
        String name = "variadicPlayers";
        String key = getKey(name);

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new PlayerArgument("a").variadic()).execute((ctx, a) -> {
                putResult(key,
                          a.stream()
                           .map(x -> x.getGameProfile()
                                      .getName())
                           .collect(Collectors.joining(",")),
                          playerName + "," + playerName);
            });
        }});

        return buildCommand(name + " " + playerName + " @p");
    }

    private String optionalVariadicOmitted() {
        String name = "optionalVariadicOmitted";
        String key = getKey(name);

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            argument(new IntegerArgument("a").variadic()
                                             .optional(List.of())).execute((ctx, a) -> {
                                                 putResult(key, String.valueOf(a), "[]");
                                             });
        }});

        return buildCommand(name);
    }
}
