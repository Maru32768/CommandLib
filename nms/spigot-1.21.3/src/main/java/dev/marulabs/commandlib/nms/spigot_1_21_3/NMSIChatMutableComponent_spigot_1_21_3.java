package dev.marulabs.commandlib.nms.spigot_1_21_3;

import dev.marulabs.commandlib.util.nms.chat.NMSIChatMutableComponent;
import dev.marulabs.commandlib.util.nms.chat.NMSTranslatableContents;
import net.minecraft.network.chat.MutableComponent;

public class NMSIChatMutableComponent_spigot_1_21_3 extends NMSIChatMutableComponent {
    public NMSIChatMutableComponent_spigot_1_21_3(Object handle) {
        super(handle, MutableComponent.class);
    }

    @Override
    public NMSTranslatableContents getContentsAsTranslatable() {
        return NMSTranslatableContents.create(((MutableComponent) requireHandle()).getContents());
    }
}
