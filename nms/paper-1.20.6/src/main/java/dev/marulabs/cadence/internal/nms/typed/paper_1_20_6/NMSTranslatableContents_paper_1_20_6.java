package dev.marulabs.cadence.internal.nms.typed.paper_1_20_6;

import dev.marulabs.cadence.internal.nms.chat.NMSTranslatableContents;
import net.minecraft.network.chat.contents.TranslatableContents;

public class NMSTranslatableContents_paper_1_20_6 extends NMSTranslatableContents {
    public NMSTranslatableContents_paper_1_20_6(Object handle) {
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
