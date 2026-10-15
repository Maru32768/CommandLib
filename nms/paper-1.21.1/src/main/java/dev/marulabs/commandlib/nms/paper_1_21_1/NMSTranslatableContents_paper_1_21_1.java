package dev.marulabs.commandlib.nms.paper_1_21_1;

import dev.marulabs.commandlib.util.nms.chat.NMSTranslatableContents;
import net.minecraft.network.chat.contents.TranslatableContents;

public class NMSTranslatableContents_paper_1_21_1 extends NMSTranslatableContents {
    public NMSTranslatableContents_paper_1_21_1(Object handle) {
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
