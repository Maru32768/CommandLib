package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.minecraft.util.ResourceLocation;

public class ResourceLocationArgument extends Argument<ResourceLocation, ResourceLocationArgument> {
    public ResourceLocationArgument(String name) {
        super(name, net.minecraft.command.arguments.ResourceLocationArgument.resourceLocation());
    }

    @Override
    public ResourceLocation cast(Object parsedArgument) {
        return (ResourceLocation) parsedArgument;
    }

    @Override
    protected ResourceLocation parseImpl(CommandContext ctx) throws ArgumentParseException {
        return net.minecraft.command.arguments.ResourceLocationArgument.getResourceLocation(ctx.getHandle(), name());
    }
}
