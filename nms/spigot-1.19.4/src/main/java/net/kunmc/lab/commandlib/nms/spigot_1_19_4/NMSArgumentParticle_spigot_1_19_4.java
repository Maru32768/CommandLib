package net.kunmc.lab.commandlib.nms.spigot_1_19_4;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentParticle;
import net.kunmc.lab.commandlib.util.nms.core.NMSParticleParam;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ParticleArgument;

public class NMSArgumentParticle_spigot_1_19_4 extends NMSArgumentParticle {
    public NMSArgumentParticle_spigot_1_19_4() {
        super(null, "commands.arguments.ArgumentParticle");
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
