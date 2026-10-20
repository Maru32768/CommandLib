package dev.marulabs.commandlib.internal.nms.typed.spigot_1_16_5;

import com.mojang.brigadier.Message;
import dev.marulabs.commandlib.internal.nms.chat.NMSChatMessage;
import net.minecraft.server.v1_16_R3.ChatMessage;

public class NMSChatMessage_spigot_1_16_5 extends NMSChatMessage {
    public NMSChatMessage_spigot_1_16_5(Message handle) {
        super(handle, ChatMessage.class);
    }

    @Override
    public String getKey() {
        return ((ChatMessage) requireHandle()).getKey();
    }

    @Override
    public Object[] getArgs() {
        return ((ChatMessage) requireHandle()).getArgs();
    }
}
