package dev.marulabs.commandlib.internal.nms.typed.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentParticle;
import dev.marulabs.commandlib.internal.nms.core.NMSParticleParam;
import net.minecraft.server.v1_16_R3.ArgumentParticle;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;

public class NMSArgumentParticle_spigot_1_16_5 extends NMSArgumentParticle {
    public NMSArgumentParticle_spigot_1_16_5() {
        super(null, ArgumentParticle.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return new ArgumentParticle();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected NMSParticleParam parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        return NMSParticleParam.create(ArgumentParticle.a(context, name));
    }
}
