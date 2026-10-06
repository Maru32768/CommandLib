package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver;
import io.papermc.paper.math.FinePosition;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
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
                               .getWorld(), pos.x(), pos.y(), pos.z());
    }
}
