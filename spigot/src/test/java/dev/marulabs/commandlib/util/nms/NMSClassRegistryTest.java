package dev.marulabs.commandlib.util.nms;

import dev.marulabs.commandlib.nms.registry_typed.TypedLookUpImpl;
import dev.marulabs.commandlib.util.bukkit.MinecraftVersion;
import dev.marulabs.commandlib.util.nms.exception.NMSClassNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NMSClassRegistryTest {
    /**
     * Fixture modules under {@code dev.marulabs.commandlib.nms}, each for its own versions.
     */
    private static final List<NMSClassRegistry.TypedModule> FIXTURE_MODULES = List.of(module("registry_typed",
                                                                                             "1.20.4"),
                                                                                      module("registry_mismatched",
                                                                                             "1.20.6"),
                                                                                      module("registry_typed",
                                                                                             "1.20.6"),
                                                                                      module("registry_unlinkable",
                                                                                             "1.18.2"),
                                                                                      module("registry_missing",
                                                                                             "1.16.5"),
                                                                                      module("registry_broken",
                                                                                             "1.19.4"));

    /**
     * The releases the spigot module supports. Each needs a typed NMS module, since the modules are the only
     * implementations.
     */
    @ParameterizedTest
    @ValueSource(strings = {"1.16.4", "1.16.5", "1.17", "1.17.1", "1.18", "1.18.1", "1.18.2", "1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4", "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6", "1.21", "1.21.1"})
    void every_supported_release_has_a_typed_module(String release) throws ReflectiveOperationException {
        MinecraftVersion version = new MinecraftVersion(release);
        Field lower = NMSClassRegistry.TypedModule.class.getDeclaredField("lowerVersion");
        Field upper = NMSClassRegistry.TypedModule.class.getDeclaredField("upperVersion");
        lower.setAccessible(true);
        upper.setAccessible(true);

        boolean covered = false;
        for (NMSClassRegistry.TypedModule module : TypedNmsModules.MODULES) {
            covered |= version.isWithin((MinecraftVersion) lower.get(module), (MinecraftVersion) upper.get(module));
        }
        assertThat(covered).as(release)
                           .isTrue();
    }

    @Test
    void find_returns_the_class_the_module_lists_for_the_look_up_class() {
        assertThat(new NMSClassRegistry(FIXTURE_MODULES).find(TypedLookUp.class,
                                                              "1.20.4")).isEqualTo(TypedLookUpImpl.class);
    }

    @Test
    void find_skips_a_module_whose_mappings_do_not_match() {
        assertThat(new NMSClassRegistry(FIXTURE_MODULES).find(TypedLookUp.class,
                                                              "1.20.6")).isEqualTo(TypedLookUpImpl.class);
    }

    @Test
    void find_fails_for_a_version_without_a_module() {
        NMSClassRegistry registry = new NMSClassRegistry(FIXTURE_MODULES);

        assertThatThrownBy(() -> registry.find(TypedLookUp.class,
                                               "1.16.3")).isInstanceOf(NMSClassNotFoundException.class)
                                                         .hasMessage("TypedLookUp has no implementation for Minecraft 1.16.3: no typed NMS module supports this "
                                                                 + "version");
    }

    @Test
    void find_fails_for_a_look_up_class_the_module_does_not_implement() {
        NMSClassRegistry registry = new NMSClassRegistry(FIXTURE_MODULES);

        assertThatThrownBy(() -> registry.find(OtherLookUp.class,
                                               "1.20.4")).isInstanceOf(NMSClassNotFoundException.class)
                                                         .hasMessage("OtherLookUp has no implementation for Minecraft 1.20.4 (registry_typed: no implementation)");
    }

    @Test
    void find_reports_why_each_module_fails() {
        NMSClassRegistry registry = new NMSClassRegistry(FIXTURE_MODULES);

        assertThatThrownBy(() -> registry.find(TypedLookUp.class,
                                               "1.18.2")).hasMessageStartingWith("TypedLookUp has no implementation for Minecraft 1.18.2 (registry_unlinkable: ")
                                                         .hasMessageContaining("org.jetbrains.annotations.NotNull");
        assertThatThrownBy(() -> registry.find(TypedLookUp.class,
                                               "1.16.5")).hasMessage("TypedLookUp has no implementation for Minecraft 1.16.5 (registry_missing: the module is not "
                                                       + "in this jar)");
        assertThatThrownBy(() -> registry.find(TypedLookUp.class,
                                               "1.19.4")).hasMessageStartingWith("TypedLookUp has no implementation for Minecraft 1.19.4 (registry_broken: "
                                                       + "java.lang.ClassFormatError");
    }

    @Test
    void find_resolves_again_for_another_version() {
        NMSClassRegistry registry = new NMSClassRegistry(FIXTURE_MODULES);

        assertThat(registry.find(TypedLookUp.class, "1.20.4")).isEqualTo(TypedLookUpImpl.class);
        assertThatThrownBy(() -> registry.find(TypedLookUp.class,
                                               "1.18.2")).isInstanceOf(NMSClassNotFoundException.class);
    }

    @Test
    void unavailable_lists_the_classes_of_matching_modules_that_do_not_resolve() {
        NMSClassRegistry registry = new NMSClassRegistry(FIXTURE_MODULES);

        assertThat(registry.unavailable("1.20.4")).isEmpty();
        assertThat(registry.unavailable("1.20.6")).isEmpty();
        assertThat(registry.unavailable("1.18.2")).singleElement()
                                                  .asString()
                                                  .startsWith("TypedLookUp has no implementation for Minecraft 1.18.2");
        assertThat(registry.unavailable("1.16.5")).containsExactly("registry_missing: the module is not in this jar");
        assertThat(registry.unavailable("1.19.4")).singleElement()
                                                  .asString()
                                                  .startsWith("registry_broken: java.lang.ClassFormatError");
        assertThat(registry.unavailable("1.16.3")).containsExactly("No typed NMS module matches Minecraft 1.16.3");
    }

    private static NMSClassRegistry.TypedModule module(String packageName, String version) {
        return new NMSClassRegistry.TypedModule(packageName, version, version);
    }

    public abstract static class TypedLookUp extends NMSClass {
        protected TypedLookUp() {
            super(null, Object.class);
        }
    }

    private abstract static class OtherLookUp extends NMSClass {
        OtherLookUp() {
            super(null, Object.class);
        }
    }
}
