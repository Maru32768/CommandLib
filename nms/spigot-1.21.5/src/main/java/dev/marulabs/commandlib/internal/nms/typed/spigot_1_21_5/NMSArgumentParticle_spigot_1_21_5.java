package dev.marulabs.commandlib.internal.nms.typed.spigot_1_21_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentParticle;
import dev.marulabs.commandlib.internal.nms.core.NMSParticleParam;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ParticleArgument;

public class NMSArgumentParticle_spigot_1_21_5 extends NMSArgumentParticle {
    public NMSArgumentParticle_spigot_1_21_5() {
        super(null, ParticleArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ParticleArgument.particle(BuildContexts.current());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected NMSParticleParam parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        return NMSParticleParam.create(ParticleArgument.getParticle(context, name));
    }
}
