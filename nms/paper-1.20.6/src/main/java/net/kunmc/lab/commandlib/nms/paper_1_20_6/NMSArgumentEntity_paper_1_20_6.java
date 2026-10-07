package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentEntity;
import net.kunmc.lab.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;

public class NMSArgumentEntity_paper_1_20_6 extends NMSArgumentEntity {
    public NMSArgumentEntity_paper_1_20_6() {
        super(null, "commands.arguments.EntityArgument");
    }

    @Override
    public ArgumentType<?> argument() {
        return EntityArgument.entity();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected org.bukkit.entity.Entity parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        try {
            return EntityArgument.getEntity(context, name).getBukkitEntity();
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
