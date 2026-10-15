package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver;
import io.papermc.paper.math.FinePosition;
import org.bukkit.Location;

/**
 * A position in the vanilla {@code vec3} syntax: {@code x y z}, relative ({@code ~}) or local ({@code ^}) coordinates.
 * The value is a location in the sender's world.
 */
@SuppressWarnings("UnstableApiUsage")
public class LocationArgument extends Argument<Location, LocationArgument> {
    /**
     * Creates the argument, which moves an integer x or z coordinate to the center of its block, as vanilla commands
     * do: {@code 1 64 1} is {@code 1.5 64 1.5}.
     */
    public LocationArgument(String name) {
        this(name, true);
    }

    /**
     * Creates the argument, which moves an integer x or z coordinate to the center of its block when
     * {@code centerIntegers} is true and keeps it otherwise: {@code 1 64 1} is {@code 1.5 64 1.5} or {@code 1 64 1}.
     */
    public LocationArgument(String name, boolean centerIntegers) {
        super(name, ArgumentTypes.finePosition(centerIntegers));
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
