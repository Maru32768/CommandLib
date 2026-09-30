package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.minecraft.core.particles.ParticleOptions;

public class ParticleArgument extends Argument<ParticleOptions, ParticleArgument> {
    public ParticleArgument(String name) {
        super(name, net.minecraft.commands.arguments.ParticleArgument.particle(BuiltInCommandBuildContext.INSTANCE));
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
