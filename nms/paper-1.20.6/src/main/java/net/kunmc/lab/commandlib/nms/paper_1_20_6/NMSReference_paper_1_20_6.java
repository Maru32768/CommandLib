package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import net.kunmc.lab.commandlib.util.nms.core.NMSHolder;
import net.minecraft.core.Holder;

public class NMSReference_paper_1_20_6 extends NMSHolder.NMSReference {
    public NMSReference_paper_1_20_6(Object handle) {
        super(handle, "core.Holder$Reference");
    }

    @Override
    public Object value() {
        return ((Holder.Reference<?>) getHandle()).value();
    }
}
