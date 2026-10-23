package dev.marulabs.commandlib;

import dev.marulabs.commandlib.argument.PlayerArgument;
import dev.marulabs.commandlib.argument.StringArgument;
import dev.marulabs.commandlib.command.CommandMiddleware;
import org.bukkit.entity.Player;

import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * {@link AnnotatedGuildCommand} written fluently.
 */
public final class FluentGuildCommand extends Command {
    public static final CommandMiddleware<CommandContext> AUDIT = (ctx, next) -> {
        ctx.sendMessage("audit");
        return next.proceed();
    };
    public static final Predicate<CommandContext> PLAYERS = ctx -> ctx.getSender() instanceof Player;
    private static final Caption DESCRIPTION = Caption.of("test.guild.description", "Manage guilds");

    public FluentGuildCommand() {
        super("guild");
        description(DESCRIPTION);
        examples("/guild red invite Steve", "/guild red kick -s Steve");
        addMiddleware(AUDIT);

        execute(ctx -> ctx.sendSuccess("Guilds: red"));
        argument(new StringArgument("guild")).child(guild -> new Command("invite") {{
            description("Invite players");
            argument(new PlayerArgument("players").variadic(3)).execute((players, ctx) -> {
                ctx.sendSuccess("Invited " + players.stream()
                                                    .map(Player::getName)
                                                    .collect(Collectors.joining(", "))
                        + " to " + guild.get());
            });
        }})
                                             .child(guild -> new Command("kick") {{
                                                 optionsAnywhere();
                                                 visibleWhen(PLAYERS);
                                                 CommandOption<Boolean, CommandContext> silent = option(Options.flag("silent",
                                                                                                                     's'));
                                                 CommandOption<Boolean, CommandContext> loud = option(Options.flag("loud",
                                                                                                                   'l')
                                                                                                             .permission(DefaultPermission.ALL));
                                                 CommandOption<String, CommandContext> reason = option(Options.string("reason",
                                                                                                                      'r',
                                                                                                                      ""));
                                                 CommandOption<Player, CommandContext> by = option(Options.of("by",
                                                                                                              'b',
                                                                                                              new PlayerArgument("by").optional(player())));
                                                 reason.requires(by);
                                                 mutuallyExclusiveOptions(silent, loud);
                                                 argument(new PlayerArgument("target")).execute((target, ctx) -> {
                                                     ctx.sendSuccess("Kicked " + target.getName() + " from "
                                                             + guild.get() + " by " + ctx.getOption(by)
                                                                                         .getName()
                                                             + " for '" + ctx.getOption(reason) + "'"
                                                             + (ctx.getOption(silent) ? " silently" : "")
                                                             + (ctx.getOption(loud) ? " loudly" : ""));
                                                 });
                                             }});
    }
}
