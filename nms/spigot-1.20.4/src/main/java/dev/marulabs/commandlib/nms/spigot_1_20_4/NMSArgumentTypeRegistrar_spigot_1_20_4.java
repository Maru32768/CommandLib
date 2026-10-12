package dev.marulabs.commandlib.nms.spigot_1_20_4;

import com.mojang.brigadier.arguments.ArgumentType;
import java.util.function.Supplier;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentTypeRegistrar;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;

public class NMSArgumentTypeRegistrar_spigot_1_20_4 extends NMSArgumentTypeRegistrar {
    public NMSArgumentTypeRegistrar_spigot_1_20_4() {
        super(ArgumentTypeInfos.class);
    }

    @Override
    public <T extends ArgumentType<?>> void registerAsGreedyString(Class<T> argumentTypeClass, Supplier<T> factory) {
        registerInArgumentTypeInfos(argumentTypeClass, ArgumentTypeInfo.class);
    }
}
