package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentVec3D;
import dev.marulabs.commandlib.internal.nms.command.NMSCommandListenerWrapper;
import dev.marulabs.commandlib.internal.nms.world.NMSVec3D;
import org.bukkit.Location;

/**
 * A position in the vanilla {@code vec3} syntax: {@code x y z}, relative ({@code ~}) or local ({@code ^}) coordinates.
 * The value is a location in the sender's world.
 */
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
        super(name,
              NMSArgumentVec3D.create()
                              .argument(centerIntegers));
    }

    @Override
    public Location cast(Object parsedArgument) {
        return ((Location) parsedArgument);
    }

    @Override
    protected Location parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        NMSVec3D vec = NMSArgumentVec3D.create()
                                       .parse(ctx.getHandle(), name());
        return new Location(NMSCommandListenerWrapper.create(ctx.getHandle()
                                                                .getSource())
                                                     .getBukkitWorld(),
                            vec.x(),
                            vec.y(),
                            vec.z());
    }
}
