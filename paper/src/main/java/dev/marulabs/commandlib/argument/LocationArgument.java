package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver;
import io.papermc.paper.math.FinePosition;
import org.bukkit.Location;

@SuppressWarnings("UnstableApiUsage")
public class LocationArgument extends Argument<Location, LocationArgument> {
    public LocationArgument(String name) {
        // Center integer x/z coordinates like vanilla commands and the Spigot implementation do (1 64 1 -> 1.5 64 1.5).
        super(name, ArgumentTypes.finePosition(true));
    }

    @Override
    public Location cast(Object parsedArgument) {
        return (Location) parsedArgument;
    }

    @Override
    protected Location parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        FinePositionResolver resolver = ctx.getHandle()
                                           .getArgument(name(), FinePositionResolver.class);
        FinePosition pos = resolver.resolve(ctx.getHandle()
                                               .getSource());
        return new Location(ctx.getHandle()
                               .getSource()
                               .getLocation()
                               .getWorld(),
                            pos.x(),
                            pos.y(),
                            pos.z());
    }
}
