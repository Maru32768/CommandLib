package net.kunmc.lab.commandlib.nms.spigot_1_20_1;

import com.mojang.brigadier.arguments.ArgumentType;
import java.util.function.Supplier;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentTypeRegistrar;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;

public class NMSArgumentTypeRegistrar_spigot_1_20_1 extends NMSArgumentTypeRegistrar {
    public NMSArgumentTypeRegistrar_spigot_1_20_1() {
        super(ArgumentTypeInfos.class);
    }

    @Override
    public <T extends ArgumentType<?>> void registerAsGreedyString(Class<T> argumentTypeClass, Supplier<T> factory) {
        registerInArgumentTypeInfos(argumentTypeClass, ArgumentTypeInfo.class);
    }
}
