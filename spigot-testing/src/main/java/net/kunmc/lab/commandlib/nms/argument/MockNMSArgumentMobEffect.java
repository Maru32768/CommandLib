package net.kunmc.lab.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.nms.world.MockNMSMobEffectList;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentMobEffect;
import net.kunmc.lab.commandlib.util.nms.world.NMSMobEffectList;
import org.bukkit.potion.PotionEffectType;

public class MockNMSArgumentMobEffect extends NMSArgumentMobEffect {
    public MockNMSArgumentMobEffect() {
        super(null, "Mock");
    }

    @Override
    public ArgumentType<?> argument() {
        // Resolved through PotionEffectType#getByName, so only keys whose path matches the Bukkit name work
        // (minecraft:speed works, minecraft:slowness does not because Bukkit calls it SLOW).
        return MockArgumentTypes.resourceKey("effect",
                                             key -> PotionEffectType.getByName(MockArgumentTypes.bukkitName(key)) != null);
    }

    @Override
    protected NMSMobEffectList parseImpl(CommandContext<?> ctx, String name) {
        return new MockNMSMobEffectList(ctx.getArgument(name, String.class));
    }
}
