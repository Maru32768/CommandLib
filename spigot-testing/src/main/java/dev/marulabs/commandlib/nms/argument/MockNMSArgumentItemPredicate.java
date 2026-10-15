package dev.marulabs.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentItemPredicate;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Reads an item id such as {@code stone} and matches items of that material. Tags, {@code *}, NBT and component tests
 * are not supported.
 */
public class MockNMSArgumentItemPredicate extends NMSArgumentItemPredicate {
    public MockNMSArgumentItemPredicate() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return MockArgumentTypes.resourceKey("item", key -> {
            Material material = Material.matchMaterial(key);
            return material != null && material.isItem();
        }, MockNMSArgumentItemPredicate::itemKeys);
    }

    @Override
    protected Predicate<ItemStack> parseImpl(CommandContext<?> ctx, String name) {
        Material material = Objects.requireNonNull(Material.matchMaterial(ctx.getArgument(name, String.class)));
        return item -> item != null && item.getType() == material;
    }

    private static Collection<String> itemKeys() {
        return Arrays.stream(Material.values())
                     .filter(x -> !x.isLegacy() && x.isItem())
                     .map(x -> x.getKey()
                                .toString())
                     .collect(Collectors.toList());
    }
}
