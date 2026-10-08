package net.kunmc.lab.commandlib.util.nms;

import net.kunmc.lab.commandlib.util.bukkit.BukkitUtil;
import net.kunmc.lab.commandlib.util.bukkit.MinecraftVersion;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentMobEffect;
import net.kunmc.lab.commandlib.util.nms.argument.v1_16_0.NMSArgumentMobEffect_v1_16_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_17_0.NMSArgumentMobEffect_v1_17_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_18_0.NMSArgumentMobEffect_v1_18_0;
import net.kunmc.lab.commandlib.util.nms.argument.v1_19_3.NMSArgumentMobEffect_v1_19_3;
import net.kunmc.lab.commandlib.util.nms.argument.v1_20_5.NMSArgumentMobEffect_v1_20_5;
import net.kunmc.lab.commandlib.util.nms.command.NMSCommandListenerWrapper;
import net.kunmc.lab.commandlib.util.nms.command.v1_20_5.NMSCommandListenerWrapper_v1_20_5;
import net.kunmc.lab.commandlib.util.nms.command.v1_20_6.NMSCommandListenerWrapper_v1_20_6;
import net.kunmc.lab.commandlib.util.nms.exception.UnregisteredNMSClassException;
import net.kunmc.lab.commandlib.util.nms.mismatchedprobe.MismatchedTypedLookUp;
import net.kunmc.lab.commandlib.util.nms.unlinkable.UnlinkableTypedLookUp;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NMSClassRegistryTest {
    private static final List<String> RELEASED_VERSIONS = List.of("1.16",
                                                                  "1.16.1",
                                                                  "1.16.2",
                                                                  "1.16.3",
                                                                  "1.16.4",
                                                                  "1.16.5",
                                                                  "1.17",
                                                                  "1.17.1",
                                                                  "1.18",
                                                                  "1.18.1",
                                                                  "1.18.2",
                                                                  "1.19",
                                                                  "1.19.1",
                                                                  "1.19.2",
                                                                  "1.19.3",
                                                                  "1.19.4",
                                                                  "1.20",
                                                                  "1.20.1",
                                                                  "1.20.2",
                                                                  "1.20.3",
                                                                  "1.20.4",
                                                                  "1.20.5",
                                                                  "1.20.6",
                                                                  "1.21",
                                                                  "1.21.1",
                                                                  "1.21.4");

    /**
     * Look-up classes that only exist for part of the supported range. Every other look-up class must cover the
     * whole supported range, so a removed or narrowed registration fails here instead of on a server.
     */
    private static final Map<String, String[]> PARTIAL_RANGES = Map.of("NMSChatMessage",
                                                                       new String[]{"1.16.0", "1.18.2"},
                                                                       "NMSCommandBuildContext",
                                                                       new String[]{"1.19.0", "9.9.9"},
                                                                       "NMSHolder",
                                                                       new String[]{"1.19.0", "9.9.9"},
                                                                       "NMSIChatMutableComponent",
                                                                       new String[]{"1.19.0", "9.9.9"},
                                                                       "NMSReference",
                                                                       new String[]{"1.19.0", "9.9.9"},
                                                                       "NMSRegistries",
                                                                       new String[]{"1.19.3", "9.9.9"},
                                                                       "NMSReloadableResources",
                                                                       new String[]{"1.19.0", "9.9.9"},
                                                                       "NMSTranslatableContents",
                                                                       new String[]{"1.19.0", "9.9.9"},
                                                                       "NMSVec2D",
                                                                       new String[]{"1.20.5", "9.9.9"});

    /**
     * Packages under {@code net.minecraft} that the version-specific implementations may reference. A misspelled
     * package only fails at runtime on the matching server version, so it is checked here.
     */
    private static final Set<String> KNOWN_MINECRAFT_PACKAGES = Set.of("commands",
                                                                       "commands.arguments",
                                                                       "commands.arguments.blocks",
                                                                       "commands.arguments.coordinates",
                                                                       "commands.arguments.item",
                                                                       "commands.synchronization",
                                                                       "core",
                                                                       "core.particles",
                                                                       "core.registries",
                                                                       "network.chat",
                                                                       "network.chat.contents",
                                                                       "resources",
                                                                       "server",
                                                                       "server.dedicated",
                                                                       "world.effect",
                                                                       "world.entity",
                                                                       "world.item",
                                                                       "world.item.enchantment",
                                                                       "world.level.block.state",
                                                                       "world.phys",
                                                                       "world.scores");

    private static List<Class<?>> nmsClasses;

    @BeforeAll
    static void loadAllNmsClasses() throws Exception {
        // Registrations happen in static initializers, so every NMS class must be initialized before inspecting them.
        nmsClasses = new ArrayList<>();
        for (String name : nmsClassNames()) {
            nmsClasses.add(Class.forName(name, true, NMSClassRegistryTest.class.getClassLoader()));
        }
    }

    @Test
    void every_released_version_resolves_to_exactly_one_implementation() {
        NMSClassRegistry.registrations()
                        .forEach((lookUpClass, registered) -> {
                            String[] expected = PARTIAL_RANGES.getOrDefault(lookUpClass.getSimpleName(),
                                                                            new String[]{"1.16.0", "9.9.9"});
                            MinecraftVersion lower = new MinecraftVersion(expected[0]);
                            MinecraftVersion upper = new MinecraftVersion(expected[1]);

                            for (String release : RELEASED_VERSIONS) {
                                MinecraftVersion version = new MinecraftVersion(release);
                                List<String> matches = registered.stream()
                                                                 .filter(x -> version.isWithin(x.lowerVersion,
                                                                                               x.upperVersion))
                                                                 .map(x -> x.clazz.getSimpleName())
                                                                 .collect(Collectors.toList());
                                if (version.isWithin(lower, upper)) {
                                    assertThat(matches).as("%s on %s", lookUpClass.getSimpleName(), release)
                                                       .hasSize(1);
                                } else {
                                    assertThat(matches).as("%s on %s", lookUpClass.getSimpleName(), release)
                                                       .isEmpty();
                                }
                            }
                        });
    }

    @ParameterizedTest
    @CsvSource({"1.16.5, NMSArgumentMobEffect_v1_16_0",
                "1.17.1, NMSArgumentMobEffect_v1_17_0",
                "1.18, NMSArgumentMobEffect_v1_18_0",
                "1.19.2, NMSArgumentMobEffect_v1_18_0",
                "1.19.3, NMSArgumentMobEffect_v1_19_3",
                "1.20.4, NMSArgumentMobEffect_v1_19_3",
                "1.20.5, NMSArgumentMobEffect_v1_20_5",
                "1.21.4, NMSArgumentMobEffect_v1_20_5"})
    void find_class_selects_implementation_for_server_version(String version, String expectedClass) {
        try (MockedStatic<BukkitUtil> bukkitUtil = Mockito.mockStatic(BukkitUtil.class)) {
            bukkitUtil.when(BukkitUtil::getMinecraftVersion)
                      .thenReturn(version);

            assertThat(NMSClassRegistry.findClass(NMSArgumentMobEffect.class)
                                       .getSimpleName()).isEqualTo(expectedClass);
        }
    }

    @Test
    void find_class_distinguishes_single_version_ranges() {
        try (MockedStatic<BukkitUtil> bukkitUtil = Mockito.mockStatic(BukkitUtil.class)) {
            bukkitUtil.when(BukkitUtil::getMinecraftVersion)
                      .thenReturn("1.20.5");
            assertThat(NMSClassRegistry.findClass(NMSCommandListenerWrapper.class)).isEqualTo(
                    NMSCommandListenerWrapper_v1_20_5.class);

            bukkitUtil.when(BukkitUtil::getMinecraftVersion)
                      .thenReturn("1.20.6");
            assertThat(NMSClassRegistry.findClass(NMSCommandListenerWrapper.class)).isEqualTo(
                    NMSCommandListenerWrapper_v1_20_6.class);
        }
    }

    @Test
    void find_class_fails_for_version_outside_registered_ranges() {
        try (MockedStatic<BukkitUtil> bukkitUtil = Mockito.mockStatic(BukkitUtil.class)) {
            bukkitUtil.when(BukkitUtil::getMinecraftVersion)
                      .thenReturn("1.15.2");

            assertThatThrownBy(() -> NMSClassRegistry.findClass(NMSArgumentMobEffect.class)).isInstanceOf(
                    UnregisteredNMSClassException.class);
        }
    }

    @Test
    void find_class_fails_for_unregistered_look_up_class() {
        assertThatThrownBy(() -> NMSClassRegistry.findClass(UnregisteredLookUp.class)).isInstanceOf(
                UnregisteredNMSClassException.class);
    }

    @ParameterizedTest
    @CsvSource({"1.16.5, TypedLookUpReflection",
                "1.18.2, TypedLookUpReflection",
                "1.20.3, TypedLookUpTyped",
                "1.20.4, TypedLookUpTyped",
                "1.20.5, TypedLookUpReflection",
                "1.20.6, TypedLookUpReflection"})
    void find_class_prefers_typed_implementation_within_its_range_and_falls_back_when_absent_mismatched_or_unlinkable(String version,
                                                                                              String expectedClass) {
        TypedLookUp.register();

        try (MockedStatic<BukkitUtil> bukkitUtil = Mockito.mockStatic(BukkitUtil.class)) {
            bukkitUtil.when(BukkitUtil::getMinecraftVersion)
                      .thenReturn(version);

            assertThat(NMSClassRegistry.findClass(TypedLookUp.class)
                                       .getSimpleName()).isEqualTo(expectedClass);
        }
    }

    @ParameterizedTest
    @CsvSource({"1.20.4, DerivedLookUp_spigot_1_20_4",
                "1.20.5, DerivedLookUpReflection",
                "1.20.6, DerivedLookUp_paper_1_20_6",
                "1.21.4, DerivedLookUpReflection"})
    void typed_registration_derives_class_names_from_modules_and_narrows_their_ranges(String version,
                                                                                      String expectedClass) {
        DerivedLookUp.register();

        try (MockedStatic<BukkitUtil> bukkitUtil = Mockito.mockStatic(BukkitUtil.class)) {
            bukkitUtil.when(BukkitUtil::getMinecraftVersion)
                      .thenReturn(version);

            assertThat(NMSClassRegistry.findClass(DerivedLookUp.class)
                                       .getSimpleName()).isEqualTo(expectedClass);
        }
    }

    @Test
    void implementations_are_registered_under_their_look_up_class() {
        assertThat(NMSClassRegistry.registrations()
                                   .get(NMSArgumentMobEffect.class)).<Class<?>>extracting(x -> x.clazz)
                                                                    .containsExactlyInAnyOrder(
                                                                            NMSArgumentMobEffect_v1_16_0.class,
                                                                            NMSArgumentMobEffect_v1_17_0.class,
                                                                            NMSArgumentMobEffect_v1_18_0.class,
                                                                            NMSArgumentMobEffect_v1_19_3.class,
                                                                            NMSArgumentMobEffect_v1_20_5.class);
    }

    @Test
    void minecraft_class_names_use_known_packages() throws Exception {
        Set<String> requested = new TreeSet<>();
        try (MockedStatic<NMSReflection> reflection = Mockito.mockStatic(NMSReflection.class)) {
            reflection.when(() -> NMSReflection.findMinecraftClass(Mockito.anyString(), Mockito.any(String[].class)))
                      .thenAnswer(invocation -> {
                          Object[] arguments = invocation.getArguments();
                          requested.add((String) arguments[0]);
                          for (int i = 1; i < arguments.length; i++) {
                              if (arguments[i] instanceof String[]) {
                                  requested.addAll(Arrays.asList((String[]) arguments[i]));
                              } else if (arguments[i] instanceof String) {
                                  requested.add((String) arguments[i]);
                              }
                          }
                          return Object.class;
                      });
            reflection.when(() -> NMSReflection.findCraftBukkitClass(Mockito.anyString()))
                      .thenReturn(Object.class);

            for (Class<?> clazz : nmsClasses) {
                if (!MinecraftClass.class.isAssignableFrom(clazz) || Modifier.isAbstract(clazz.getModifiers())) {
                    continue;
                }
                for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                    try {
                        constructor.setAccessible(true);
                        constructor.newInstance(new Object[constructor.getParameterCount()]);
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                        // Some constructors need a real handle; the class name is requested before that matters.
                    }
                }
            }
        }

        List<String> unknownPackages = requested.stream()
                                                .filter(x -> x.contains("."))
                                                .map(x -> x.substring(0, x.lastIndexOf('.')))
                                                .filter(x -> !KNOWN_MINECRAFT_PACKAGES.contains(x))
                                                .distinct()
                                                .collect(Collectors.toList());

        assertThat(requested).contains("commands.arguments.ArgumentMobEffect");
        assertThat(unknownPackages).isEmpty();
    }

    private static List<String> nmsClassNames() throws URISyntaxException, java.io.IOException {
        Path root = Paths.get(NMSClass.class.getProtectionDomain()
                                            .getCodeSource()
                                            .getLocation()
                                            .toURI());
        Path nmsDirectory = root.resolve("net/kunmc/lab/commandlib/util/nms");
        try (Stream<Path> paths = Files.walk(nmsDirectory)) {
            return paths.filter(x -> x.toString()
                                      .endsWith(".class"))
                        .map(x -> root.relativize(x)
                                      .toString()
                                      .replace(File.separatorChar, '.')
                                      .replaceAll("[.]class$", ""))
                        .sorted()
                        .collect(Collectors.toList());
        }
    }

    private abstract static class UnregisteredLookUp extends NMSClass {
        UnregisteredLookUp() {
            super(null, Object.class);
        }
    }

    public abstract static class TypedLookUp extends NMSClass {
        private static final AtomicBoolean REGISTERED = new AtomicBoolean();

        protected TypedLookUp() {
            super(null, Object.class);
        }

        /**
         * Registers once with the full range, so the range check above still sees exactly one reflection
         * implementation whichever test runs first.
         */
        static void register() {
            if (!REGISTERED.compareAndSet(false, true)) {
                return;
            }
            NMSClassRegistry.register(TypedLookUp.class, TypedLookUpReflection.class, "1.16.0", "9.9.9");
            NMSClassRegistry.registerTyped(TypedLookUp.class, TypedLookUpTyped.class.getName(), "1.20.3", "1.20.4");
            NMSClassRegistry.registerTyped(TypedLookUp.class,
                                           "net.kunmc.lab.commandlib.nms.missing.TypedLookUpMissing",
                                           "1.16.4",
                                           "1.16.5");
            NMSClassRegistry.registerTyped(TypedLookUp.class,
                                           MismatchedTypedLookUp.class.getName(),
                                           "1.20.6",
                                           "1.20.6");
            NMSClassRegistry.registerTyped(TypedLookUp.class,
                                           UnlinkableTypedLookUp.class.getName(),
                                           "1.18.2",
                                           "1.18.2");
        }
    }

    public abstract static class DerivedLookUp extends NMSClass {
        private static final AtomicBoolean REGISTERED = new AtomicBoolean();

        protected DerivedLookUp() {
            super(null, Object.class);
        }

        static void register() {
            if (!REGISTERED.compareAndSet(false, true)) {
                return;
            }
            NMSClassRegistry.register(DerivedLookUp.class, DerivedLookUpReflection.class, "1.16.0", "9.9.9");
            NMSClassRegistry.registerTyped(DerivedLookUp.class, "1.16.0", "1.20.4");
            NMSClassRegistry.registerTyped(DerivedLookUp.class, "1.20.6", "9.9.9");
        }
    }

    static class DerivedLookUpReflection extends DerivedLookUp {
    }

    static class TypedLookUpReflection extends TypedLookUp {
    }

    static class TypedLookUpTyped extends TypedLookUp {
    }
}
