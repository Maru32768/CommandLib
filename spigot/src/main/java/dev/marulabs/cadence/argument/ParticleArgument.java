package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import dev.marulabs.cadence.internal.nms.argument.NMSArgumentParticle;
import dev.marulabs.cadence.internal.nms.resources.NMSCraftParticle;
import org.bukkit.Particle;

public class ParticleArgument extends Argument<Particle, ParticleArgument> {
    public ParticleArgument(String name) {
        super(name,
              NMSArgumentParticle.create()
                                 .argument());
    }

    @Override
    public Particle cast(Object parsedArgument) {
        return ((Particle) parsedArgument);
    }

    @Override
    protected Particle parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return NMSCraftParticle.create()
                               .toBukkit(NMSArgumentParticle.create()
                                                            .parse(ctx.getHandle(), name()));
    }
}
