package dev.marulabs.commandlib.internal.nms.typed.spigot_1_21_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentChatComponent;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ComponentArgument;
import org.bukkit.craftbukkit.v1_21_R4.util.CraftChatMessage;

public class NMSArgumentChatComponent_spigot_1_21_5 extends NMSArgumentChatComponent {
    public NMSArgumentChatComponent_spigot_1_21_5() {
        super(null, ComponentArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ComponentArgument.textComponent(BuildContexts.current());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected BaseComponent parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        return ComponentSerializer.parse(CraftChatMessage.toJSON(ComponentArgument.getRawComponent(context, name)))[0];
    }
}
