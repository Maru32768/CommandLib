package net.kunmc.lab.commandlib.argument;

//? if >=1.19.3 {
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
//? if neoforge {
/*import net.neoforged.neoforge.server.ServerLifecycleHooks;
*///?} else
import net.minecraftforge.server.ServerLifecycleHooks;
//? if >=1.20.5 {
/*import java.util.Optional;
import java.util.stream.Stream;
*///?}

// Argument types are created when a command is declared, which may be before the server exists.
// This context resolves registries and applies the running server's enabled feature flags
// lazily at parse/suggestion time instead of at construction time.
final class BuiltInCommandBuildContext implements CommandBuildContext {
    static final CommandBuildContext INSTANCE = new BuiltInCommandBuildContext();

    private static final RegistryAccess BUILT_IN = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private BuiltInCommandBuildContext() {
    }

    //? if >=1.20.5 {
    /*@Override
    public Stream<ResourceKey<? extends Registry<?>>> listRegistries() {
        return registries().listRegistries();
    }

    @Override
    public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
        return Optional.of(new LazyRegistryLookup<>(key));
    }

    // Data-driven registries such as enchantments only exist in the server's registries.
    private static HolderLookup.Provider registries() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return BUILT_IN;
        }
        return server.registryAccess();
    }

    // Argument types look up their registry when they are created, so resolve it again on every use.
    private static final class LazyRegistryLookup<T> implements HolderLookup.RegistryLookup.Delegate<T> {
        private final ResourceKey<? extends Registry<? extends T>> key;

        private LazyRegistryLookup(ResourceKey<? extends Registry<? extends T>> key) {
            this.key = key;
        }

        @Override
        public ResourceKey<? extends Registry<? extends T>> key() {
            return key;
        }

        @Override
        public HolderLookup.RegistryLookup<T> parent() {
            return filterByEnabledFeatures(key, registries().lookupOrThrow(key));
        }
    }

    private static <T> HolderLookup.RegistryLookup<T> filterByEnabledFeatures(ResourceKey<? extends Registry<? extends T>> key,
                                                                              HolderLookup.RegistryLookup<T> lookup) {
    *///?} else {
    @Override
    public <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<T>> key) {
        return filterByEnabledFeatures(key,
                                       BUILT_IN.registryOrThrow(key)
                                               .asLookup());
    }

    private static <T> HolderLookup<T> filterByEnabledFeatures(ResourceKey<? extends Registry<? extends T>> key,
                                                               HolderLookup.RegistryLookup<T> lookup) {
    //?}
        if (!FeatureElement.FILTERED_REGISTRIES.contains(key)) {
            return lookup;
        }
        return lookup.filterElements(x -> ((FeatureElement) x).isEnabled(enabledFeatures()));
    }

    private static FeatureFlagSet enabledFeatures() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return FeatureFlags.DEFAULT_FLAGS;
        }
        return server.getWorldData()
                     .enabledFeatures();
    }
}
//?}
