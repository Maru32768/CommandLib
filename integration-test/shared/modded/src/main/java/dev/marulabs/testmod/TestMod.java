package dev.marulabs.testmod;

//? if fabric {
/*import net.fabricmc.api.ModInitializer;
*///?} elif neoforge {
/*import net.neoforged.fml.common.Mod;
*///?} else
import net.minecraftforge.fml.common.Mod;

//? if fabric {
/*public final class TestMod implements ModInitializer {
    public static final String MOD_ID = "commandlibtest";

    @Override
    public void onInitialize() {
        new TestMain().register();
    }
}
*///?} else {
@Mod(TestMod.MOD_ID)
public final class TestMod {
    public static final String MOD_ID = "commandlibtest";

    public TestMod() {
        new TestMain().register();
    }
}
//?}
