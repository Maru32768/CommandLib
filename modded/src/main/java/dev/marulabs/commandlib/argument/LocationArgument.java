package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.Location;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * A position in the vanilla {@code vec3} syntax: {@code x y z}, relative ({@code ~}) or local ({@code ^}) coordinates.
 * The value is a location in the sender's level, with the sender's rotation.
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
        super(name, Vec3Argument.vec3(centerIntegers));
    }

    @Override
    public Location cast(Object parsedArgument) {
        return ((Location) parsedArgument);
    }

    @Override
    protected Location parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        Vec3 vec = Vec3Argument.getVec3(ctx.getHandle(), name());
        Location loc = new Location(ctx.getWorld(), vec.x, vec.y, vec.z);

        Entity sender = ctx.getEntity();
        if (sender != null) {
            //? if >=1.17 {
            loc.setYaw(sender.getYRot());
            loc.setPitch(sender.getXRot());
            //?} else {
            /*loc.setYaw(sender.yRot);
            loc.setPitch(sender.xRot);
            *///?}
        }

        return loc;
    }
}
