package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;

public class ParticleArgument extends Argument<ParticleOptions, ParticleArgument> {
    public ParticleArgument(String name) {
        super(name, ResourceLocationArgument.id());
    }

    @Override
    public ParticleOptions cast(Object parsedArgument) {
        return ((ParticleOptions) parsedArgument);
    }

    @Override
    protected ParticleOptions parseImpl(CommandContext ctx) throws ArgumentParseException {
        return (ParticleOptions) BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocationArgument.getId(ctx.getHandle(),
                                                                                                    name()));
    }
}
