package dev.marulabs.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentTypeRegistrar;

import java.util.function.Supplier;

public class MockNMSArgumentTypeRegistrar extends NMSArgumentTypeRegistrar {
    public MockNMSArgumentTypeRegistrar() {
        super(Object.class);
    }

    @Override
    public <T extends ArgumentType<?>> void registerAsGreedyString(Class<T> argumentTypeClass, Supplier<T> factory) {
        // CommandTester executes Brigadier directly and never serializes nodes to clients.
    }
}
