package dev.marulabs.commandlib.internal.nms.typed.paper_1_21_1;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentChatComponent;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ComponentArgument;
import org.bukkit.craftbukkit.util.CraftChatMessage;

public class NMSArgumentChatComponent_paper_1_21_1 extends NMSArgumentChatComponent {
    public NMSArgumentChatComponent_paper_1_21_1() {
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
        return ComponentSerializer.parse(CraftChatMessage.toJSON(ComponentArgument.getComponent(context, name)))[0];
    }
}
