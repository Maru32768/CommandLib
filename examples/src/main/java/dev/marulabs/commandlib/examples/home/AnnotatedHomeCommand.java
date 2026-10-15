package dev.marulabs.commandlib.examples.home;

import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.annotation.AnnotatedCommands;
import dev.marulabs.commandlib.annotation.Command;
import dev.marulabs.commandlib.annotation.Subcommand;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link FluentHomeCommand} written with annotations. There are no annotations for the captions or a localized
 * description, so {@link #create} sets them on the parsed command.
 */
@Command("home")
public final class AnnotatedHomeCommand {
    private final Set<String> homes = new HashSet<>();

    public static dev.marulabs.commandlib.Command create() {
        dev.marulabs.commandlib.Command command = AnnotatedCommands.parse(new AnnotatedHomeCommand());
        command.captions(HomeCaptions.create());
        command.description(ctx -> ctx.caption(HomeCaptions.DESCRIPTION));
        return command;
    }

    @Subcommand("go")
    public void go(CommandContext ctx, String name) throws CommandPrerequisiteException {
        if (!homes.contains(name)) {
            throw new CommandPrerequisiteException(HomeCaptions.UNKNOWN, Map.of("name", name));
        }
        ctx.sendSuccess(HomeCaptions.TELEPORTED, Map.of("name", name));
    }

    @Subcommand("set")
    public void set(CommandContext ctx, String name) {
        homes.add(name);
        ctx.sendSuccess(HomeCaptions.SET, Map.of("name", name));
    }
}
