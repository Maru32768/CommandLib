package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentDimension;
import net.kunmc.lab.commandlib.util.nms.exception.UncheckedCommandSyntaxException;
import net.minecraft.server.v1_16_R3.ArgumentDimension;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import org.bukkit.World;

public class NMSArgumentDimension_spigot_1_16_5 extends NMSArgumentDimension {
    public NMSArgumentDimension_spigot_1_16_5() {
        super(null, "ArgumentDimension");
    }

    @Override
    public ArgumentType<?> argument() {
        return ArgumentDimension.a();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected World parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        try {
            return ArgumentDimension.a(context, name).getWorld();
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
}
