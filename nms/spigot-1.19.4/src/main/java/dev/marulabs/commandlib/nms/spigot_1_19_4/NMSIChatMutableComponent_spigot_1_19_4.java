package dev.marulabs.commandlib.nms.spigot_1_19_4;

import dev.marulabs.commandlib.util.nms.chat.NMSIChatMutableComponent;
import dev.marulabs.commandlib.util.nms.chat.NMSTranslatableContents;
import net.minecraft.network.chat.MutableComponent;

public class NMSIChatMutableComponent_spigot_1_19_4 extends NMSIChatMutableComponent {
    public NMSIChatMutableComponent_spigot_1_19_4(Object handle) {
        super(handle, MutableComponent.class);
    }

    @Override
    public NMSTranslatableContents getContentsAsTranslatable() {
        return NMSTranslatableContents.create(((MutableComponent) getHandle()).getContents());
    }
}
