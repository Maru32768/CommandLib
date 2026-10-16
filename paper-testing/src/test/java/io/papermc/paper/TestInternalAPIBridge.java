package io.papermc.paper;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.world.damagesource.CombatEntry;
import io.papermc.paper.world.damagesource.FallLocationType;
import org.bukkit.block.Biome;
import org.bukkit.damage.DamageEffect;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Lets tests mock {@link Biome} without a server: its {@code CUSTOM} constant asks this bridge, which Paper loads
 * through {@link java.util.ServiceLoader}. Nothing else is supported.
 */
@SuppressWarnings({"removal", "UnstableApiUsage"})
public final class TestInternalAPIBridge implements InternalAPIBridge {
    @Override
    public @Nullable Biome constructLegacyCustomBiome() {
        return null;
    }

    @Override
    public DamageEffect getDamageEffect(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CombatEntry createCombatEntry(LivingEntity entity, DamageSource damageSource, float damage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CombatEntry createCombatEntry(DamageSource damageSource,
                                         float damage,
                                         @Nullable FallLocationType fallLocationType,
                                         float fallDistance) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Predicate<CommandSourceStack> restricted(Predicate<CommandSourceStack> predicate) {
        throw new UnsupportedOperationException();
    }
}
