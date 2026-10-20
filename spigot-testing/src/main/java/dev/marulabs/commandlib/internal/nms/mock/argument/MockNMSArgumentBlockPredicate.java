package dev.marulabs.commandlib.internal.nms.mock.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentBlockPredicate;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Reads a block id such as {@code stone} and matches blocks of that material. Tags, block states and NBT are not
 * supported.
 */
@ApiStatus.Internal
public class MockNMSArgumentBlockPredicate extends NMSArgumentBlockPredicate {
    public MockNMSArgumentBlockPredicate() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return MockArgumentTypes.resourceKey("block", key -> {
            Material material = Material.matchMaterial(key);
            return material != null && material.isBlock();
        }, MockNMSArgumentBlockPredicate::blockKeys);
    }

    @Override
    protected Predicate<Block> parseImpl(CommandContext<?> ctx, String name) {
        Material material = Objects.requireNonNull(Material.matchMaterial(ctx.getArgument(name, String.class)));
        return block -> block.getType() == material;
    }

    private static Collection<String> blockKeys() {
        return Arrays.stream(Material.values())
                     .filter(x -> !x.isLegacy() && x.isBlock())
                     .map(x -> x.getKey()
                                .toString())
                     .collect(Collectors.toList());
    }
}
