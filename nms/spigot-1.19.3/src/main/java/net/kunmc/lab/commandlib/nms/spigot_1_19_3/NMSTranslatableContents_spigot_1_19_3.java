package net.kunmc.lab.commandlib.nms.spigot_1_19_3;

import net.kunmc.lab.commandlib.util.nms.chat.NMSTranslatableContents;
import net.minecraft.network.chat.contents.TranslatableContents;

public class NMSTranslatableContents_spigot_1_19_3 extends NMSTranslatableContents {
    public NMSTranslatableContents_spigot_1_19_3(Object handle) {
        super(handle, "network.chat.contents.TranslatableContents");
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
