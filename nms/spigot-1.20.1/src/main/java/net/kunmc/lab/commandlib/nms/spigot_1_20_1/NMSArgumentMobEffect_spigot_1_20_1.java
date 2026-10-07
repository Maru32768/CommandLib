package net.kunmc.lab.commandlib.nms.spigot_1_20_1;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentMobEffect;
import net.kunmc.lab.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.world.NMSMobEffectList;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;

public class NMSArgumentMobEffect_spigot_1_20_1 extends NMSArgumentMobEffect {
    public NMSArgumentMobEffect_spigot_1_20_1() {
        super(null, "commands.arguments.ResourceArgument");
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
