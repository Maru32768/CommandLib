package dev.marulabs.commandlib.internal.nms.mock.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.internal.nms.argument.NMSArgumentMobEffect;
import dev.marulabs.commandlib.internal.nms.mock.world.MockNMSMobEffectList;
import dev.marulabs.commandlib.internal.nms.world.NMSMobEffectList;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class MockNMSArgumentMobEffect extends NMSArgumentMobEffect {
    public MockNMSArgumentMobEffect() {
        super(null, Object.class);
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
