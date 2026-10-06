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
import net.minecraftforge.server.ServerLifecycleHooks;

// Argument types are created when a command is declared, which may be before the server exists.
// This context resolves built-in registries and applies the running server's enabled feature flags
// lazily at parse/suggestion time instead of at construction time.
final class BuiltInCommandBuildContext implements CommandBuildContext {
    static final CommandBuildContext INSTANCE = new BuiltInCommandBuildContext();

    private static final RegistryAccess BUILT_IN = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private BuiltInCommandBuildContext() {
    }

    @Override
    public <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<T>> key) {
        HolderLookup.RegistryLookup<T> lookup = BUILT_IN.registryOrThrow(key)
                                                        .asLookup();
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
