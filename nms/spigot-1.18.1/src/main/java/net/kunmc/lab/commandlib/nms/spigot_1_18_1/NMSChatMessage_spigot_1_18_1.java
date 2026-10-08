package net.kunmc.lab.commandlib.nms.spigot_1_18_1;

import com.mojang.brigadier.Message;
import net.kunmc.lab.commandlib.util.nms.chat.NMSChatMessage;
import net.minecraft.network.chat.TranslatableComponent;

public class NMSChatMessage_spigot_1_18_1 extends NMSChatMessage {
    public NMSChatMessage_spigot_1_18_1(Message handle) {
        super(handle, "network.chat.ChatMessage");
    }

    @Override
    public String getKey() {
        return ((TranslatableComponent) getHandle()).getKey();
    }

    @Override
    public Object[] getArgs() {
        return ((TranslatableComponent) getHandle()).getArgs();
    }
}
