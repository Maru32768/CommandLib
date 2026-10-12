package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
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
