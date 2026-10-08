package net.kunmc.lab.commandlib.nms.spigot_1_20_2;

import net.kunmc.lab.commandlib.util.nms.chat.NMSIChatMutableComponent;
import net.kunmc.lab.commandlib.util.nms.chat.NMSTranslatableContents;
import net.minecraft.network.chat.MutableComponent;

public class NMSIChatMutableComponent_spigot_1_20_2 extends NMSIChatMutableComponent {
    public NMSIChatMutableComponent_spigot_1_20_2(Object handle) {
        super(handle, "network.chat.IChatMutableComponent");
    }

    @Override
    public NMSTranslatableContents getContentsAsTranslatable() {
        return NMSTranslatableContents.create(((MutableComponent) getHandle()).getContents());
    }
}
