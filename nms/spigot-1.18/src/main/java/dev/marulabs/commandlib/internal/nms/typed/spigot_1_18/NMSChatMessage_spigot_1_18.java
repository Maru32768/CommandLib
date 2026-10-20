package dev.marulabs.commandlib.internal.nms.typed.spigot_1_18;

import com.mojang.brigadier.Message;
import dev.marulabs.commandlib.internal.nms.chat.NMSChatMessage;
import net.minecraft.network.chat.TranslatableComponent;

public class NMSChatMessage_spigot_1_18 extends NMSChatMessage {
    public NMSChatMessage_spigot_1_18(Message handle) {
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
