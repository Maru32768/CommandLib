package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import net.minecraft.resources.ResourceLocation;

public class ResourceLocationArgument extends Argument<ResourceLocation, ResourceLocationArgument> {
    public ResourceLocationArgument(String name) {
        super(name, net.minecraft.commands.arguments.ResourceLocationArgument.id());
    }

    @Override
    public ResourceLocation cast(Object parsedArgument) {
        return (ResourceLocation) parsedArgument;
    }

    @Override
    protected ResourceLocation parseImpl(CommandContext ctx) throws ArgumentParseException {
        return net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx.getHandle(), name());
    }
}
