package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentEnchantment;
import net.kunmc.lab.commandlib.util.nms.world.NMSEnchantment;
import net.minecraft.server.v1_16_R3.ArgumentEnchantment;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;

public class NMSArgumentEnchantment_spigot_1_16_5 extends NMSArgumentEnchantment {
    public NMSArgumentEnchantment_spigot_1_16_5() {
        super(null, "ArgumentEnchantment");
    }

    @Override
    public ArgumentType<?> argument() {
        return ArgumentEnchantment.a();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected NMSEnchantment parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        return NMSEnchantment.create(ArgumentEnchantment.a(context, name));
    }
}
