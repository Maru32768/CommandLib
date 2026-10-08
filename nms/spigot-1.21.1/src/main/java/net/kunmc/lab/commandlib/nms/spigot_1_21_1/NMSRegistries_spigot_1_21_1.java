package net.kunmc.lab.commandlib.nms.spigot_1_21_1;

import net.kunmc.lab.commandlib.util.nms.core.NMSRegistries;
import net.kunmc.lab.commandlib.util.nms.resources.NMSResourceKey;
import net.minecraft.core.registries.Registries;

public class NMSRegistries_spigot_1_21_1 extends NMSRegistries {
    public NMSRegistries_spigot_1_21_1() {
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
