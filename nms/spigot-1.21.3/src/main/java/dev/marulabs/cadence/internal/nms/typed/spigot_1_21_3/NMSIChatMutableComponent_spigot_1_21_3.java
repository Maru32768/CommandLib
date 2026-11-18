package dev.marulabs.cadence.internal.nms.typed.spigot_1_21_3;

import dev.marulabs.cadence.internal.nms.chat.NMSIChatMutableComponent;
import dev.marulabs.cadence.internal.nms.chat.NMSTranslatableContents;
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
