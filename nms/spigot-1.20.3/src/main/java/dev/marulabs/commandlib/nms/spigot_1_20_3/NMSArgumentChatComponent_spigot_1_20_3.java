package dev.marulabs.commandlib.nms.spigot_1_20_3;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.BungeeChatJson;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentChatComponent;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ComponentArgument;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftChatMessage;

public class NMSArgumentChatComponent_spigot_1_20_3 extends NMSArgumentChatComponent {
    public NMSArgumentChatComponent_spigot_1_20_3() {
        super(null, ComponentArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return ComponentArgument.textComponent();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected BaseComponent parseImpl(CommandContext<?> ctx, String name) {
        CommandContext<CommandSourceStack> context = (CommandContext<CommandSourceStack>) ctx;
        return ComponentSerializer.parse(BungeeChatJson.withStringEntityIds(CraftChatMessage.toJSON(ComponentArgument.getComponent(context, name))))[0];
    }
}
