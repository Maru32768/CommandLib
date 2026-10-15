package dev.marulabs.commandlib.nms.spigot_1_18_2;

import com.mojang.brigadier.Message;
import dev.marulabs.commandlib.util.nms.chat.NMSChatMessage;
import net.minecraft.network.chat.TranslatableComponent;

public class NMSChatMessage_spigot_1_18_2 extends NMSChatMessage {
    public NMSChatMessage_spigot_1_18_2(Message handle) {
        super(handle, TranslatableComponent.class);
    }

    @Override
    public String getKey() {
        return ((TranslatableComponent) requireHandle()).getKey();
    }

    @Override
    public Object[] getArgs() {
        return ((TranslatableComponent) requireHandle()).getArgs();
    }
}
