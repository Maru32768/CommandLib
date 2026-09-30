package net.kunmc.lab.commandlib.argument;

import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
import net.minecraft.particles.IParticleData;

public class ParticleArgument extends Argument<IParticleData, ParticleArgument> {
    public ParticleArgument(String name) {
        super(name, net.minecraft.command.arguments.ParticleArgument.particle());
    }

    @Override
    public IParticleData cast(Object parsedArgument) {
        return ((IParticleData) parsedArgument);
    }

    @Override
    protected IParticleData parseImpl(CommandContext ctx) throws ArgumentParseException {
        return net.minecraft.command.arguments.ParticleArgument.getParticle(ctx.getHandle(), name());
    }
}
