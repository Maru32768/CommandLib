package dev.marulabs.commandlib.nms.spigot_1_21_5;

import dev.marulabs.commandlib.util.nms.chat.NMSTranslatableContents;
import net.minecraft.network.chat.contents.TranslatableContents;

public class NMSTranslatableContents_spigot_1_21_5 extends NMSTranslatableContents {
    public NMSTranslatableContents_spigot_1_21_5(Object handle) {
        super(handle, TranslatableContents.class);
    }

    @Override
    public String getKey() {
        return ((TranslatableContents) getHandle()).getKey();
    }

    @Override
    public Object[] getArgs() {
        return ((TranslatableContents) getHandle()).getArgs();
    }
}
