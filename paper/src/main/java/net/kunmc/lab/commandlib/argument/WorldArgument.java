package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
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
