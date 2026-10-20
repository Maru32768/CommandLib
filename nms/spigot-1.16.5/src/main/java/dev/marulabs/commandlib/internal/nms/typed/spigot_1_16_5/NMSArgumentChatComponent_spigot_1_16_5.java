package dev.marulabs.commandlib.internal.nms.typed.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentChatComponent;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.v1_16_R3.ArgumentChatComponent;
import net.minecraft.server.v1_16_R3.CommandListenerWrapper;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftChatMessage;

public class NMSArgumentChatComponent_spigot_1_16_5 extends NMSArgumentChatComponent {
    public NMSArgumentChatComponent_spigot_1_16_5() {
        super(null, ArgumentChatComponent.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ArgumentChatComponent.a();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected BaseComponent parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandListenerWrapper> context = (CommandContext<CommandListenerWrapper>) ctx;
        return ComponentSerializer.parse(CraftChatMessage.toJSON(ArgumentChatComponent.a(context, name)))[0];
    }
}
