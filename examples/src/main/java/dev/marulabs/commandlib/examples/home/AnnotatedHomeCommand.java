package dev.marulabs.commandlib.examples.home;

import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.annotation.CaptionBundle;
import dev.marulabs.commandlib.annotation.Command;
import dev.marulabs.commandlib.annotation.Description;
import dev.marulabs.commandlib.annotation.Subcommand;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link FluentHomeCommand} written with annotations: {@link CaptionBundle} loads the same bundle as
 * {@link HomeCaptions#create()}, and the description is the caption {@link HomeCaptions#DESCRIPTION};
 * {@code AnnotatedCommands.parse(new AnnotatedHomeCommand())}.
 */
@Command("home")
@CaptionBundle("lang/homes")
@Description(value = "Manage your homes", caption = "example.home.description")
public final class AnnotatedHomeCommand {
    private final Set<String> homes = new HashSet<>();

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
