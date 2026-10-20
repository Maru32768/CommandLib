package dev.marulabs.commandlib.internal.nms.typed.paper_1_20_6;

import dev.marulabs.commandlib.internal.nms.chat.NMSIChatMutableComponent;
import dev.marulabs.commandlib.internal.nms.chat.NMSTranslatableContents;
import net.minecraft.network.chat.MutableComponent;

public class NMSIChatMutableComponent_paper_1_20_6 extends NMSIChatMutableComponent {
    public NMSIChatMutableComponent_paper_1_20_6(Object handle) {
        super(handle, MutableComponent.class);
    }

    @Override
    public NMSTranslatableContents getContentsAsTranslatable() {
        return NMSTranslatableContents.create(((MutableComponent) requireHandle()).getContents());
    }
}
