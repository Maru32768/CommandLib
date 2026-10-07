package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import net.kunmc.lab.commandlib.util.nms.chat.NMSIChatMutableComponent;
import net.kunmc.lab.commandlib.util.nms.chat.NMSTranslatableContents;
import net.minecraft.network.chat.MutableComponent;

public class NMSIChatMutableComponent_paper_1_20_6 extends NMSIChatMutableComponent {
    public NMSIChatMutableComponent_paper_1_20_6(Object handle) {
        super(handle, "network.chat.MutableComponent");
    }

    @Override
    public NMSTranslatableContents getContentsAsTranslatable() {
        return NMSTranslatableContents.create(((MutableComponent) getHandle()).getContents());
    }
}
