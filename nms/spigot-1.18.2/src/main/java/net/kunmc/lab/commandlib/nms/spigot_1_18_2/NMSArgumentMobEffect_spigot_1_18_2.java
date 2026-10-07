package net.kunmc.lab.commandlib.nms.spigot_1_18_2;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentMobEffect;
import net.kunmc.lab.commandlib.util.nms.world.NMSMobEffectList;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.MobEffectArgument;

public class NMSArgumentMobEffect_spigot_1_18_2 extends NMSArgumentMobEffect {
    public NMSArgumentMobEffect_spigot_1_18_2() {
        super(null, "commands.arguments.ArgumentMobEffect");
    }

    @Override
    public ArgumentType<?> argument() {
        return MobEffectArgument.effect();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected NMSMobEffectList parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        return NMSMobEffectList.create(MobEffectArgument.getEffect(context, name));
    }
}
