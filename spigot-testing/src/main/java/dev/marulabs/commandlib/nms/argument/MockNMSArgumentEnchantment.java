package dev.marulabs.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.nms.world.MockNMSEnchantment;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentEnchantment;
import dev.marulabs.commandlib.util.nms.world.NMSEnchantment;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

public class MockNMSArgumentEnchantment extends NMSArgumentEnchantment {
    public MockNMSArgumentEnchantment() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return MockArgumentTypes.resourceKey("enchantment",
                                             key -> Enchantment.getByKey(NamespacedKey.fromString(key)) != null);
    }

    @Override
    protected NMSEnchantment parseImpl(CommandContext<?> ctx, String name) {
        return new MockNMSEnchantment(ctx.getArgument(name, String.class));
    }
}
