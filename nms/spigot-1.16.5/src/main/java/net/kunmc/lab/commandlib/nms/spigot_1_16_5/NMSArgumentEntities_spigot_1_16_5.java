package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.stream.Collectors;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentEntities;
import net.kunmc.lab.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import net.minecraft.server.v1_16_R3.ArgumentEntity;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import org.bukkit.entity.Entity;

public class NMSArgumentEntities_spigot_1_16_5 extends NMSArgumentEntities {
    public NMSArgumentEntities_spigot_1_16_5() {
        super(null, "ArgumentEntity");
    }

    @Override
    public ArgumentType<?> argument() {
        return ArgumentEntity.multipleEntities();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected List<Entity> parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        try {
            return ArgumentEntity.b(context, name).stream()
                                 .map(x -> ((Entity) x.getBukkitEntity()))
                                 .collect(Collectors.toList());
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
