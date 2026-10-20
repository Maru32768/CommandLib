package dev.marulabs.commandlib.internal.nms.mock.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentItemStack;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentPredicateItemStack;
import org.bukkit.Material;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class MockNMSArgumentItemStack extends NMSArgumentItemStack {
    public MockNMSArgumentItemStack() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        // NBT and item components are not supported by the mock.
        return MockArgumentTypes.resourceKey("item", key -> {
            Material material = Material.matchMaterial(key);
            return material != null && material.isItem();
        });
    }

    @Override
    protected NMSArgumentPredicateItemStack parseImpl(CommandContext<?> ctx, String name) {
        return new MockNMSArgumentPredicateItemStack(ctx.getArgument(name, String.class));
    }
}
