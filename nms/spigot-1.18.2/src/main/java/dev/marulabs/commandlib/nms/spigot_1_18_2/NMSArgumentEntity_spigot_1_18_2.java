package dev.marulabs.commandlib.nms.spigot_1_18_2;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentEntity;
import dev.marulabs.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;

public class NMSArgumentEntity_spigot_1_18_2 extends NMSArgumentEntity {
    public NMSArgumentEntity_spigot_1_18_2() {
        super(null, EntityArgument.class);
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
