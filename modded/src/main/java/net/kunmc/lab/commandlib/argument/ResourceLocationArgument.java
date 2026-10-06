package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
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
