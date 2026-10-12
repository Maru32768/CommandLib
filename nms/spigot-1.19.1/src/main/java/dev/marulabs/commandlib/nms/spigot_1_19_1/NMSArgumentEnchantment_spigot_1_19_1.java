package dev.marulabs.commandlib.nms.spigot_1_19_1;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentEnchantment;
import dev.marulabs.commandlib.util.nms.world.NMSEnchantment;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ItemEnchantmentArgument;

public class NMSArgumentEnchantment_spigot_1_19_1 extends NMSArgumentEnchantment {
    public NMSArgumentEnchantment_spigot_1_19_1() {
        super(null, ItemEnchantmentArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ItemEnchantmentArgument.enchantment();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected NMSEnchantment parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        return NMSEnchantment.create(ItemEnchantmentArgument.getEnchantment(context, name));
    }
}
