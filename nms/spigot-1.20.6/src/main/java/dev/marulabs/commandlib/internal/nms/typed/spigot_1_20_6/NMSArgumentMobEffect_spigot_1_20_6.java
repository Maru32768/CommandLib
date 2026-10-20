package dev.marulabs.commandlib.internal.nms.typed.spigot_1_20_6;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentMobEffect;
import dev.marulabs.commandlib.internal.nms.exception.UncheckedCommandSyntaxException;
import dev.marulabs.commandlib.internal.nms.world.NMSMobEffectList;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;

public class NMSArgumentMobEffect_spigot_1_20_6 extends NMSArgumentMobEffect {
    public NMSArgumentMobEffect_spigot_1_20_6() {
        super(null, ResourceArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ResourceArgument.resource(BuildContexts.current(), Registries.MOB_EFFECT);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected NMSMobEffectList parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        try {
            return NMSMobEffectList.create(ResourceArgument.getMobEffect(context, name)
                                                           .value());
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
