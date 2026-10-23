package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.IntegerArgument;
import dev.marulabs.commandlib.argument.PlayerArgument;
import dev.marulabs.commandlib.argument.StringArgument;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link AnnotatedArenaCommand} written fluently. Subcommands are added in the alphabetical order the annotation
 * parser adds methods in.
 */
public final class FluentArenaCommand extends Command {
    private final List<String> arenas = new ArrayList<>(List.of("lobby"));

    public FluentArenaCommand() {
        super("arena");
        addAliases("ar");
        description("Manage arenas");

        addChildren(new Command("create") {{
            description("Create an arena");
            permission(DefaultPermission.ALL, "Create arenas");
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f')
                                                                         .description("Replace an existing arena"));
            argument(new StringArgument("name"),
                     new IntegerArgument("size", 1, 64).optional(16)).execute((name, size, ctx) -> {
                         arenas.add(name);
                         ctx.sendSuccess("Created " + name + " with " + size + " slots"
                                 + (ctx.getOption(force) ? " (forced)" : ""));
                     });
        }}, new Command("delete") {{
            confirm();
            argument(new StringArgument("name").setSuggestionProvider(sb -> arenas.forEach(sb::suggest))).execute((name,
                                                                                                                   ctx) -> {
                arenas.remove(name);
                ctx.sendSuccess("Deleted " + name);
            });
        }}, new Command("invite") {{
            argument(new StringArgument("arena"),
                     new PlayerArgument("players").variadic()).execute((arena, players, ctx) -> {
                         ctx.sendSuccess("Invited " + players.stream()
                                                             .map(x -> x.getName()
                                                                        .getString())
                                                             .collect(Collectors.joining(", "))
                                 + " to " + arena);
                     });
        }}, new Command("join") {{
            argument(new StringArgument("arena"),
                     new PlayerArgument("player").optional(player())).execute((arena, player, ctx) -> {
                         ctx.sendSuccess(player.getName()
                                               .getString()
                                 + " joined " + arena);
                     });
        }}, new Command("list") {{
            cooldown(Duration.ofMinutes(1));
            CommandOption<Integer, CommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100)
                                                                         .description("Maximum count"));
            execute(ctx -> ctx.sendSuccess(String.join(", ",
                                                       arenas.subList(0,
                                                                      Math.min(ctx.getOption(limit),
                                                                               arenas.size())))));
        }}, new Command("stats") {{
            argument(new StringArgument("arena")).executeAsync((arena, ctx) -> ctx.supplyAsync(arena::length)
                                                                                  .thenAccept(games -> ctx.sendSuccess(arena
                                                                                          + " has " + games
                                                                                          + " games")));
        }});
    }
}
