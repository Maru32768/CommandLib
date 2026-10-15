package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.server.level.ServerLevel;

/**
 * A level of the server in the vanilla {@code dimension} syntax, such as {@code minecraft:overworld} or
 * {@code minecraft:the_nether}; the server suggests its levels.
 */
public class WorldArgument extends Argument<ServerLevel, WorldArgument> {
    public WorldArgument(String name) {
        super(name, DimensionArgument.dimension());
    }

    @Override
    public ServerLevel cast(Object parsedArgument) {
        return (ServerLevel) parsedArgument;
    }

    @Override
    protected ServerLevel parseImpl(CommandContext ctx) throws CommandSyntaxException {
        return DimensionArgument.getDimension(ctx.getHandle(), name());
    }
}
