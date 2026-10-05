package net.kunmc.lab.testmod;

import net.minecraftforge.fml.common.Mod;

@Mod(TestMod.MOD_ID)
public final class TestMod {
    public static final String MOD_ID = "commandlibtest";

    public TestMod() {
        new TestMain().register();
    }
}
