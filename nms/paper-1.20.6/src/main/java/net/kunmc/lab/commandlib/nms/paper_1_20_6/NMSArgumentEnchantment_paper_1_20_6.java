package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentEnchantment;
import net.kunmc.lab.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.world.NMSEnchantment;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;

public class NMSArgumentEnchantment_paper_1_20_6 extends NMSArgumentEnchantment {
    public NMSArgumentEnchantment_paper_1_20_6() {
        super(null, "commands.arguments.ResourceArgument");
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
