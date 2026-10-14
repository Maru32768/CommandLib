package dev.marulabs.commandlib.nms.spigot_1_21_3;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentEnchantment;
import dev.marulabs.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import dev.marulabs.commandlib.util.nms.world.NMSEnchantment;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;

public class NMSArgumentEnchantment_spigot_1_21_3 extends NMSArgumentEnchantment {
    public NMSArgumentEnchantment_spigot_1_21_3() {
        super(null, ResourceArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ResourceArgument.resource(BuildContexts.current(), Registries.ENCHANTMENT);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected NMSEnchantment parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        try {
            return NMSEnchantment.create(ResourceArgument.getEnchantment(context, name)
                                                         .value());
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
