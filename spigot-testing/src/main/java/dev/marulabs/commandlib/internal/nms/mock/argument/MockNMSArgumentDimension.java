package dev.marulabs.commandlib.internal.nms.mock.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentDimension;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class MockNMSArgumentDimension extends NMSArgumentDimension {
    public MockNMSArgumentDimension() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        // A server reads any resource location here and resolves the dimension later.
        return MockArgumentTypes.resourceKey("dimension", key -> true);
    }

    @Override
    protected World parseImpl(CommandContext<?> ctx, String name) {
        String key = ctx.getArgument(name, String.class);
        World world = CommandTester.getFakeWorld(key);
        if (world == null) {
            throw MockArgumentTypes.resolveError("Unknown dimension '" + key + "'");
        }
        return world;
    }
}
