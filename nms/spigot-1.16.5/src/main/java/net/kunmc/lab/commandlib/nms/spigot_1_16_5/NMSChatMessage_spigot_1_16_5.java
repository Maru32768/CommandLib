package net.kunmc.lab.commandlib.nms.spigot_1_16_5;

import com.mojang.brigadier.Message;
import net.kunmc.lab.commandlib.util.nms.chat.NMSChatMessage;
import net.minecraft.server.v1_16_R3.ChatMessage;

public class NMSChatMessage_spigot_1_16_5 extends NMSChatMessage {
    public NMSChatMessage_spigot_1_16_5(Message handle) {
        super(handle, "ChatMessage");
    }

    @Override
    public String getKey() {
        return ((ChatMessage) getHandle()).getKey();
    }

    @Override
    public Object[] getArgs() {
        return ((ChatMessage) getHandle()).getArgs();
    }
}
