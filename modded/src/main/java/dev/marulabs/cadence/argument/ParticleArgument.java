package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import net.minecraft.core.particles.ParticleOptions;

public class ParticleArgument extends Argument<ParticleOptions, ParticleArgument> {
    public ParticleArgument(String name) {
        //? if >=1.19.3 {
        super(name, net.minecraft.commands.arguments.ParticleArgument.particle(BuiltInCommandBuildContext.INSTANCE));
        //?} else
        /*super(name, net.minecraft.commands.arguments.ParticleArgument.particle());*/
    }

    @Override
    public ParticleOptions cast(Object parsedArgument) {
        return ((ParticleOptions) parsedArgument);
    }

    @Override
    protected ParticleOptions parseImpl(CommandContext ctx) throws ArgumentParseException {
        return net.minecraft.commands.arguments.ParticleArgument.getParticle(ctx.getHandle(), name());
    }
}
