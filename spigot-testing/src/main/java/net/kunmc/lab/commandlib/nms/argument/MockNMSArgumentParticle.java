package net.kunmc.lab.commandlib.nms.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.nms.core.MockNMSParticleParam;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentParticle;
import net.kunmc.lab.commandlib.util.nms.core.NMSParticleParam;
import org.bukkit.Particle;

public class MockNMSArgumentParticle extends NMSArgumentParticle {
    public MockNMSArgumentParticle() {
        super(null, "Mock");
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
