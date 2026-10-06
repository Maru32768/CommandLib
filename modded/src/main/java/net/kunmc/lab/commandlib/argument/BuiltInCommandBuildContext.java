package net.kunmc.lab.commandlib.argument;

//? if >=1.19.3 {
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
//? if neoforge {
/*import net.neoforged.neoforge.server.ServerLifecycleHooks;
*///?} else
import net.minecraftforge.server.ServerLifecycleHooks;
//? if >=1.20.5 {
/*import com.mojang.serialization.Lifecycle;
import net.minecraft.resources.RegistryDataLoader;

import java.util.Set;
import java.util.stream.Collectors;
*///?}

import java.util.Optional;
import java.util.stream.Stream;

// Argument types are created when a command is declared, which may be before the server exists.
// This context resolves registries and applies the running server's enabled feature flags
// lazily at parse/suggestion time instead of at construction time.
final class BuiltInCommandBuildContext implements CommandBuildContext {
    static final CommandBuildContext INSTANCE = new BuiltInCommandBuildContext();

    private static final RegistryAccess BUILT_IN = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private BuiltInCommandBuildContext() {
    }

    //? if >=1.20.5 {
    /*// Data-driven registries such as enchantments only exist in the server's registries.
    private static final Set<ResourceKey<? extends Registry<?>>> DATA_DRIVEN =
            Stream.concat(RegistryDataLoader.WORLDGEN_REGISTRIES.stream(), RegistryDataLoader.DIMENSION_REGISTRIES.stream())
                  .map(RegistryDataLoader.RegistryData::key)
                  .collect(Collectors.toSet());

    @Override
    public Stream<ResourceKey<? extends Registry<?>>> listRegistries() {
        Stream<ResourceKey<? extends Registry<?>>> keys = Stream.concat(BUILT_IN.listRegistries(), DATA_DRIVEN.stream());
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            keys = Stream.concat(keys,
                                 server.registryAccess()
                                       .listRegistries());
        }
        return keys.distinct();
    }

    @Override
    public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
        if (listRegistries().noneMatch(key::equals)) {
            return Optional.empty();
        }
        return Optional.of(new LazyRegistryLookup<>(key));
    }

    private static HolderLookup.Provider registries() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return BUILT_IN;
        }
        return server.registryAccess();
    }

    // Argument types look up their registry when they are created, so resolve it again on every use.
    // The resolved lookup is reused while the registries and the enabled feature flags stay the same.
    private static final class LazyRegistryLookup<T> implements HolderLookup.RegistryLookup.Delegate<T> {
        private final ResourceKey<? extends Registry<? extends T>> key;
        private volatile Resolved<T> resolved;

        private LazyRegistryLookup(ResourceKey<? extends Registry<? extends T>> key) {
            this.key = key;
        }

        @Override
        public ResourceKey<? extends Registry<? extends T>> key() {
            return key;
        }

        @Override
        public HolderLookup.RegistryLookup<T> parent() {
            HolderLookup.Provider registries = registries();
            FeatureFlagSet flags = enabledFeatures();
            Resolved<T> current = resolved;
            if (current == null || current.registries() != registries || !current.flags()
                                                                                 .equals(flags)) {
                // Without a server, data-driven registries are missing, so parsing fails with an unknown element.
                HolderLookup.RegistryLookup<T> lookup = CommandBuildContext.simple(registries, flags)
                                                                           .<T>lookup(key)
                                                                           .orElseGet(() -> new EmptyRegistryLookup<>(key));
                current = new Resolved<>(registries, flags, lookup);
                resolved = current;
            }
            return current.lookup();
        }
    }

    private record Resolved<T>(HolderLookup.Provider registries,
                               FeatureFlagSet flags,
                               HolderLookup.RegistryLookup<T> lookup) {
    }

    private record EmptyRegistryLookup<T>(ResourceKey<? extends Registry<? extends T>> key)
            implements HolderLookup.RegistryLookup<T> {
        @Override
        public Lifecycle registryLifecycle() {
            return Lifecycle.stable();
        }

        @Override
        public Stream<Holder.Reference<T>> listElements() {
            return Stream.empty();
        }

        @Override
        public Stream<HolderSet.Named<T>> listTags() {
            return Stream.empty();
        }

        @Override
        public Optional<Holder.Reference<T>> get(ResourceKey<T> key) {
            return Optional.empty();
        }

        @Override
        public Optional<HolderSet.Named<T>> get(TagKey<T> key) {
            return Optional.empty();
        }
    }
    *///?} else {
    @Override
    public <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<T>> key) {
        return new LazyHolderLookup<>(BUILT_IN.registryOrThrow(key)
                                              .asLookup());
    }

    // Argument types look up their registry when they are created, so filter it again on every use.
    // The filtered lookup is reused while the enabled feature flags stay the same.
    private static final class LazyHolderLookup<T> implements HolderLookup<T> {
        private final HolderLookup.RegistryLookup<T> registry;
        private volatile Filtered<T> filtered;

        private LazyHolderLookup(HolderLookup.RegistryLookup<T> registry) {
            this.registry = registry;
        }

        private HolderLookup<T> current() {
            FeatureFlagSet flags = enabledFeatures();
            Filtered<T> current = filtered;
            if (current == null || !current.flags()
                                           .equals(flags)) {
                current = new Filtered<>(flags, registry.filterFeatures(flags));
                filtered = current;
            }
            return current.lookup();
        }

        @Override
        public Stream<Holder.Reference<T>> listElements() {
            return current().listElements();
        }

        @Override
        public Stream<HolderSet.Named<T>> listTags() {
            return current().listTags();
        }

        @Override
        public Optional<Holder.Reference<T>> get(ResourceKey<T> key) {
            return current().get(key);
        }

        @Override
        public Optional<HolderSet.Named<T>> get(TagKey<T> key) {
            return current().get(key);
        }
    }

    private record Filtered<T>(FeatureFlagSet flags, HolderLookup<T> lookup) {
    }
    //?}

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
