package dev.marulabs.commandlib.examples.home;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.StringArgument;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Use case 6, per-locale messages: {@code /home set <name>} and {@code /home go <name>} reply in the sender's
 * locale, and so do the description and the headings of the generated help.
 */
public final class FluentHomeCommand extends Command {
    public FluentHomeCommand() {
        super("home");
        captions(HomeCaptions.create());
        description(ctx -> ctx.caption(HomeCaptions.DESCRIPTION));
        Set<String> homes = new HashSet<>();

        addChildren(new Command("go") {{
            StringArgument name = new StringArgument("name");
            // The untyped executor, since a typed one cannot throw the checked CommandPrerequisiteException.
            argument(name).execute(ctx -> {
                if (!homes.contains(ctx.getArgument(name))) {
                    throw new CommandPrerequisiteException(HomeCaptions.UNKNOWN,
                                                           Map.of("name", ctx.getArgument(name)));
                }
                ctx.sendSuccess(HomeCaptions.TELEPORTED, Map.of("name", ctx.getArgument(name)));
            });
        }}, new Command("set") {{
            argument(new StringArgument("name")).execute((name, ctx) -> {
                homes.add(name);
                ctx.sendSuccess(HomeCaptions.SET, Map.of("name", name));
            });
        }});
    }
}
