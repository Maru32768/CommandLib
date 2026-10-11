package net.kunmc.lab.commandlib.nms.paper_1_21_1;

import net.kunmc.lab.commandlib.util.nms.chat.NMSTranslatableContents;
import net.minecraft.network.chat.contents.TranslatableContents;

public class NMSTranslatableContents_paper_1_21_1 extends NMSTranslatableContents {
    public NMSTranslatableContents_paper_1_21_1(Object handle) {
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
