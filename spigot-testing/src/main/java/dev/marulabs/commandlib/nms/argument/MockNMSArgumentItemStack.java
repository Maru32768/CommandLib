package dev.marulabs.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentItemStack;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentPredicateItemStack;
import org.bukkit.Material;

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
