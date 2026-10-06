package net.kunmc.lab.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentTile;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentTileLocation;
import org.bukkit.Material;

public class MockNMSArgumentTile extends NMSArgumentTile {
    public MockNMSArgumentTile() {
        super(null, "Mock");
    }

    @Override
    public ArgumentType<?> argument() {
        // Block states such as oak_stairs[facing=east] are not supported by the mock.
        return MockArgumentTypes.resourceKey("block", key -> {
            Material material = Material.matchMaterial(key);
            return material != null && material.isBlock();
        });
    }

    @Override
    protected NMSArgumentTileLocation parseImpl(CommandContext<?> ctx, String name) {
        return new MockNMSArgumentTileLocation(ctx.getArgument(name, String.class));
    }
}
