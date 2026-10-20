package dev.marulabs.commandlib.internal.nms.typed.spigot_1_20_1;

import dev.marulabs.commandlib.internal.nms.chat.NMSTranslatableContents;
import net.minecraft.network.chat.contents.TranslatableContents;

public class NMSTranslatableContents_spigot_1_20_1 extends NMSTranslatableContents {
    public NMSTranslatableContents_spigot_1_20_1(Object handle) {
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
