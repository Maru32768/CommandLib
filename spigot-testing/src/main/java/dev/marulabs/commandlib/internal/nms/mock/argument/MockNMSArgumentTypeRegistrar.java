package dev.marulabs.commandlib.internal.nms.mock.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentTypeRegistrar;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

@ApiStatus.Internal
public class MockNMSArgumentTypeRegistrar extends NMSArgumentTypeRegistrar {
    public MockNMSArgumentTypeRegistrar() {
        super(Object.class);
    }

    @Override
    public <T extends ArgumentType<?>> void registerAsGreedyString(Class<T> argumentTypeClass, Supplier<T> factory) {
        // CommandTester executes Brigadier directly and never serializes nodes to clients.
    }
}
