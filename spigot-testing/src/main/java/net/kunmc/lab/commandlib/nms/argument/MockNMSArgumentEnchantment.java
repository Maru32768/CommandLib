package net.kunmc.lab.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.nms.world.MockNMSEnchantment;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentEnchantment;
import net.kunmc.lab.commandlib.util.nms.world.NMSEnchantment;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

public class MockNMSArgumentEnchantment extends NMSArgumentEnchantment {
    public MockNMSArgumentEnchantment() {
        super(null, "Mock");
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
