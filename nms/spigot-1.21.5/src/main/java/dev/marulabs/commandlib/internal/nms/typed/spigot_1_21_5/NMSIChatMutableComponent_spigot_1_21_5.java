package dev.marulabs.commandlib.internal.nms.typed.spigot_1_21_5;

import dev.marulabs.commandlib.internal.nms.chat.NMSIChatMutableComponent;
import dev.marulabs.commandlib.internal.nms.chat.NMSTranslatableContents;
import net.minecraft.network.chat.MutableComponent;

public class NMSIChatMutableComponent_spigot_1_21_5 extends NMSIChatMutableComponent {
    public NMSIChatMutableComponent_spigot_1_21_5(Object handle) {
        super(handle, MutableComponent.class);
    }

    @Override
    public NMSTranslatableContents getContentsAsTranslatable() {
        return NMSTranslatableContents.create(((MutableComponent) requireHandle()).getContents());
    }
}
