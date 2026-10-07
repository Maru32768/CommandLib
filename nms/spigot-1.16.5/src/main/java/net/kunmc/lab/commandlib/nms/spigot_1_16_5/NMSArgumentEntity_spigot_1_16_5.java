package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentEntity;
import net.kunmc.lab.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import net.minecraft.server.v1_16_R3.ArgumentEntity;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import org.bukkit.entity.Entity;

public class NMSArgumentEntity_spigot_1_16_5 extends NMSArgumentEntity {
    public NMSArgumentEntity_spigot_1_16_5() {
        super(null, "ArgumentEntity");
    }

    @Override
    public ArgumentType<?> argument() {
        return ArgumentEntity.a();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Entity parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        try {
            return ArgumentEntity.a(context, name).getBukkitEntity();
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
