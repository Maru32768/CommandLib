package net.kunmc.lab.commandlib.nms.spigot_1_20_4;

import net.kunmc.lab.commandlib.util.nms.core.NMSHolder;
import net.minecraft.core.Holder;

public class NMSReference_spigot_1_20_4 extends NMSHolder.NMSReference {
    public NMSReference_spigot_1_20_4(Object handle) {
        super(handle, "core.Holder$c");
    }

    @Override
    public Object value() {
        return ((Holder.Reference<?>) getHandle()).value();
    }
}
