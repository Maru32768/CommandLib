package dev.marulabs.commandlib.nms.spigot_1_18;

import com.mojang.brigadier.arguments.ArgumentType;
import java.util.function.Supplier;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentTypeRegistrar;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.resources.ResourceLocation;

public class NMSArgumentTypeRegistrar_spigot_1_18 extends NMSArgumentTypeRegistrar {
    public NMSArgumentTypeRegistrar_spigot_1_18() {
        super(ArgumentTypes.class);
    }

    @Override
    public <T extends ArgumentType<?>> void registerAsGreedyString(Class<T> argumentTypeClass, Supplier<T> factory) {
        registerInArgumentRegistry(argumentTypeClass,
                                   factory,
                                   ArgumentSerializer.class,
                                   new ResourceLocation("brigadier:string"));
    }
}
