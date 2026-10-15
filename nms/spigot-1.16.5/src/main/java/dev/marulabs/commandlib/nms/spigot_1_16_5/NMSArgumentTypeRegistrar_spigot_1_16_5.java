package dev.marulabs.commandlib.nms.spigot_1_16_5;

import com.mojang.brigadier.arguments.ArgumentType;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentTypeRegistrar;
import java.util.function.Supplier;
import net.minecraft.server.v1_16_R3.ArgumentRegistry;
import net.minecraft.server.v1_16_R3.ArgumentSerializer;
import net.minecraft.server.v1_16_R3.MinecraftKey;

public class NMSArgumentTypeRegistrar_spigot_1_16_5 extends NMSArgumentTypeRegistrar {
    public NMSArgumentTypeRegistrar_spigot_1_16_5() {
        super(ArgumentRegistry.class);
    }

    @Override
    public <T extends ArgumentType<?>> void registerAsGreedyString(Class<T> argumentTypeClass, Supplier<T> factory) {
        registerInArgumentRegistry(argumentTypeClass,
                                   factory,
                                   ArgumentSerializer.class,
                                   new MinecraftKey("brigadier:string"));
    }
}
