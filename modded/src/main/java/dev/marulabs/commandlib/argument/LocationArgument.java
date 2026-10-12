package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.Location;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class LocationArgument extends Argument<Location, LocationArgument> {
    public LocationArgument(String name) {
        super(name, Vec3Argument.vec3());
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
