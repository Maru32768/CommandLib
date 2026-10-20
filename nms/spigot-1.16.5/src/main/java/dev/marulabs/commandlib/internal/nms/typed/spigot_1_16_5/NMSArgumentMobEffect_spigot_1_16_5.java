package dev.marulabs.commandlib.internal.nms.typed.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentMobEffect;
import dev.marulabs.commandlib.internal.nms.exception.UncheckedCommandSyntaxException;
import dev.marulabs.commandlib.internal.nms.world.NMSMobEffectList;
import net.minecraft.server.v1_16_R3.ArgumentMobEffect;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;

public class NMSArgumentMobEffect_spigot_1_16_5 extends NMSArgumentMobEffect {
    public NMSArgumentMobEffect_spigot_1_16_5() {
        super(null, ArgumentMobEffect.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return new ArgumentMobEffect();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected NMSMobEffectList parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        try {
            return NMSMobEffectList.create(ArgumentMobEffect.a(context, name));
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
