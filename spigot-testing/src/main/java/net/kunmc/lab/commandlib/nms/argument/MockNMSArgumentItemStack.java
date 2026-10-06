package net.kunmc.lab.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentItemStack;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentPredicateItemStack;
import org.bukkit.Material;

public class MockNMSArgumentItemStack extends NMSArgumentItemStack {
    public MockNMSArgumentItemStack() {
        super(null, "Mock");
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
