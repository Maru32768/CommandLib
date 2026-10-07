package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentParticle;
import net.kunmc.lab.commandlib.util.nms.core.NMSParticleParam;
import net.minecraft.server.v1_16_R3.ArgumentParticle;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;

public class NMSArgumentParticle_spigot_1_16_5 extends NMSArgumentParticle {
    public NMSArgumentParticle_spigot_1_16_5() {
        super(null, "ArgumentParticle");
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
