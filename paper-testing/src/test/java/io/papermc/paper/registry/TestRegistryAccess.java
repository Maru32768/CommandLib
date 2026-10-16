package io.papermc.paper.registry;

import org.bukkit.Keyed;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;
import org.mockito.Mockito;

/**
 * Lets tests mock registry values such as {@code Attribute} or {@code Enchantment} without a server. Their classes read
 * {@link Registry} constants while initializing, and Paper loads the {@link RegistryAccess} behind those through
 * {@link java.util.ServiceLoader}. A registry is an empty mock, so the constants of those classes are null.
 */
@SuppressWarnings({"unchecked", "removal", "UnstableApiUsage"})
public final class TestRegistryAccess implements RegistryAccess {
    @Override
    public <T extends Keyed> Registry<T> getRegistry(Class<T> type) {
        return Mockito.mock(Registry.class);
    }

    @Override
    public <T extends Keyed> Registry<T> getRegistry(RegistryKey<T> registryKey) {
        if (RegistryKey.MOB_EFFECT.equals(registryKey)) {
            // PotionEffectType keeps its constants in a BiMap, which rejects a second null, so each one is a mock.
            return Mockito.mock(Registry.class,
                                invocation -> invocation.getMethod()
                                                        .getReturnType() == Keyed.class
                                        ? Mockito.mock(PotionEffectType.class)
                                        : Mockito.RETURNS_DEFAULTS.answer(invocation));
        }
        return Mockito.mock(Registry.class);
    }
}
