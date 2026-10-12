package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentVec3D;
import dev.marulabs.commandlib.util.nms.command.NMSCommandListenerWrapper;
import dev.marulabs.commandlib.util.nms.world.NMSVec3D;
import org.bukkit.Location;

public class LocationArgument extends Argument<Location, LocationArgument> {
    public LocationArgument(String name) {
        super(name,
              NMSArgumentVec3D.create()
                              .argument());
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
