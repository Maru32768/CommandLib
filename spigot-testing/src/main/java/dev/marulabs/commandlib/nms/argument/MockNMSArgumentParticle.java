package dev.marulabs.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.marulabs.commandlib.nms.core.MockNMSParticleParam;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentParticle;
import dev.marulabs.commandlib.util.nms.core.NMSParticleParam;
import org.bukkit.Particle;

public class MockNMSArgumentParticle extends NMSArgumentParticle {
    public MockNMSArgumentParticle() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        // Resolved by Bukkit enum name, so keys whose path differs from the Bukkit name (minecraft:dust is
        // REDSTONE) and particle options are not supported.
        return MockArgumentTypes.resourceKey("particle type", key -> {
            try {
                Particle.valueOf(MockArgumentTypes.bukkitName(key));
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        });
    }

    @Override
    protected NMSParticleParam parseImpl(CommandContext<?> ctx, String name) {
        return new MockNMSParticleParam(ctx.getArgument(name, String.class));
    }
}
