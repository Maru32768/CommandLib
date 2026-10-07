package net.kunmc.lab.commandlib.nms.spigot_1_18_2;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentEnchantment;
import net.kunmc.lab.commandlib.util.nms.world.NMSEnchantment;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ItemEnchantmentArgument;

public class NMSArgumentEnchantment_spigot_1_18_2 extends NMSArgumentEnchantment {
    public NMSArgumentEnchantment_spigot_1_18_2() {
        super(null, "commands.arguments.ArgumentEnchantment");
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
