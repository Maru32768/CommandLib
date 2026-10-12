package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentParticle;
import dev.marulabs.commandlib.util.nms.resources.NMSCraftParticle;
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
