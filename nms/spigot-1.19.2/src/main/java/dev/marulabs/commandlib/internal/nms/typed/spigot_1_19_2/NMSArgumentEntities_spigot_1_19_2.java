package dev.marulabs.commandlib.internal.nms.typed.spigot_1_19_2;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentEntities;
import dev.marulabs.commandlib.internal.nms.exception.UncheckedCommandSyntaxException;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;

public class NMSArgumentEntities_spigot_1_19_2 extends NMSArgumentEntities {
    public NMSArgumentEntities_spigot_1_19_2() {
        super(null, EntityArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return EntityArgument.entities();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected List<org.bukkit.entity.Entity> parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        try {
            return EntityArgument.getEntities(context, name).stream()
                                 .map(x -> ((org.bukkit.entity.Entity) x.getBukkitEntity()))
                                 .collect(Collectors.toList());
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
