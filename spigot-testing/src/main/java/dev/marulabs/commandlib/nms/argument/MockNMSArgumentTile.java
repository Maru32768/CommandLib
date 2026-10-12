package dev.marulabs.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentTile;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentTileLocation;
import org.bukkit.Material;

public class MockNMSArgumentTile extends NMSArgumentTile {
    public MockNMSArgumentTile() {
        super(null, Object.class);
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
