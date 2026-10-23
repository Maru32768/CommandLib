package dev.marulabs.commandlib.examples.home;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.StringArgument;
import dev.marulabs.commandlib.exception.CommandPrerequisiteException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link FluentHomeCommand} written with the Java builder.
 */
public final class BuilderHomeCommand {
    private BuilderHomeCommand() {
    }

    public static Command create() {
        Set<String> homes = new HashSet<>();
        StringArgument goName = new StringArgument("name");
        return Command.builder("home")
                      .captions(HomeCaptions.create())
                      .description(HomeCaptions.DESCRIPTION)
                      .addChild(Command.builder("go")
                                       .argument(goName, branch -> branch.execute(ctx -> {
                                           if (!homes.contains(ctx.getArgument(goName))) {
                                               throw new CommandPrerequisiteException(HomeCaptions.UNKNOWN,
                                                                                      Map.of("name",
                                                                                             ctx.getArgument(goName)));
                                           }
                                           ctx.sendSuccess(HomeCaptions.TELEPORTED,
                                                           Map.of("name", ctx.getArgument(goName)));
                                       })))
                      .addChild(Command.builder("set")
                                       .argument(new StringArgument("name"), branch -> branch.execute((ctx, name) -> {
                                           homes.add(name);
                                           ctx.sendSuccess(HomeCaptions.SET, Map.of("name", name));
                                       })))
                      .build();
    }
}
