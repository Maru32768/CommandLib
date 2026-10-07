package net.kunmc.lab.commandlib.nms.paper_1_20_6;

import net.kunmc.lab.commandlib.util.nms.core.NMSRegistries;
import net.kunmc.lab.commandlib.util.nms.resources.NMSResourceKey;
import net.minecraft.core.registries.Registries;

public class NMSRegistries_paper_1_20_6 extends NMSRegistries {
    public NMSRegistries_paper_1_20_6() {
        super(null, "core.registries.Registries");
    }

    @Override
    public NMSResourceKey enchantment() {
        return NMSResourceKey.create(Registries.ENCHANTMENT);
    }

    @Override
    public NMSResourceKey mobEffect() {
        return NMSResourceKey.create(Registries.MOB_EFFECT);
    }
}
