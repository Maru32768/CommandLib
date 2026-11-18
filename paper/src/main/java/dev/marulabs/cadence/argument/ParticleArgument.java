package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Particle;

@SuppressWarnings("UnstableApiUsage")
public class ParticleArgument extends Argument<Particle, ParticleArgument> {
    public ParticleArgument(String name) {
        super(name, ArgumentTypes.resource(RegistryKey.PARTICLE_TYPE));
    }

    @Override
    public Particle cast(Object parsedArgument) {
        return (Particle) parsedArgument;
    }

    @Override
    protected Particle parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ctx.getHandle()
                  .getArgument(name(), Particle.class);
    }
}
