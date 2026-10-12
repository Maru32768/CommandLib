package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.World;

@SuppressWarnings("UnstableApiUsage")
public class WorldArgument extends Argument<World, WorldArgument> {
    public WorldArgument(String name) {
        super(name, ArgumentTypes.world());
    }

    @Override
    public World cast(Object parsedArgument) {
        return (World) parsedArgument;
    }

    @Override
    protected World parseImpl(CommandContext ctx) throws CommandSyntaxException, ArgumentParseException {
        World world = ctx.getHandle()
                         .getArgument(name(), World.class);
        if (world == null) {
            throw ArgumentParseException.ofIncorrectInput(name(), ctx, ctx.getInput(name()));
        }
        return world;
    }
}
