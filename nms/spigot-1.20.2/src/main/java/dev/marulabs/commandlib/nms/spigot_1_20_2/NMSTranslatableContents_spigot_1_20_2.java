package dev.marulabs.commandlib.nms.spigot_1_20_2;

import dev.marulabs.commandlib.util.nms.chat.NMSTranslatableContents;
import net.minecraft.network.chat.contents.TranslatableContents;

public class NMSTranslatableContents_spigot_1_20_2 extends NMSTranslatableContents {
    public NMSTranslatableContents_spigot_1_20_2(Object handle) {
        super(handle, TranslatableContents.class);
    }

    @Override
    public String getKey() {
        return ((TranslatableContents) requireHandle()).getKey();
    }

    @Override
    public Object[] getArgs() {
        return ((TranslatableContents) requireHandle()).getArgs();
    }
}
