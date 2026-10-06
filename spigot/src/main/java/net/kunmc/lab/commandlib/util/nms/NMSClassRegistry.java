package net.kunmc.lab.commandlib.util.nms;

import net.kunmc.lab.commandlib.util.bukkit.BukkitUtil;
import net.kunmc.lab.commandlib.util.bukkit.MinecraftVersion;
import net.kunmc.lab.commandlib.util.nms.exception.UnregisteredNMSClassException;

import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class NMSClassRegistry {
    private static final Map<Class<? extends NMSClass>, Deque<RegisteredClass>> CLASS_TO_DEQUE_MAP = new ConcurrentHashMap<>();

    public static <T extends NMSClass> void register(Class<T> lookUpClass,
                                                     Class<? extends T> targetClass,
                                                     String lowerVersion,
                                                     String upperVersion) {
        Objects.requireNonNull(lookUpClass);
        Objects.requireNonNull(targetClass);

        CLASS_TO_DEQUE_MAP.computeIfAbsent(lookUpClass, x -> new ConcurrentLinkedDeque<>())
                          .addFirst(new RegisteredClass(targetClass,
                                                        new MinecraftVersion(lowerVersion),
                                                        new MinecraftVersion(upperVersion)));
    }

    public static <T extends Class<? extends NMSClass>> T findClass(T clazz) {
        Deque<RegisteredClass> deque = CLASS_TO_DEQUE_MAP.get(clazz);
        if (deque == null) {
            throw new UnregisteredNMSClassException(clazz + " is unregistered.");
        }

        for (RegisteredClass registeredClass : deque) {
            boolean isWithin = new MinecraftVersion(BukkitUtil.getMinecraftVersion()).isWithin(registeredClass.lowerVersion,
                                                                                               registeredClass.upperVersion);
            if (isWithin) {
                return ((T) registeredClass.clazz);
            }
        }

        throw new UnregisteredNMSClassException(clazz + " is unregistered.");
    }

    /**
     * Returns the registered implementations of each look-up class, most recently registered first. Exposed for
     * tests that check the version ranges for gaps and overlaps.
     */
    static Map<Class<? extends NMSClass>, List<RegisteredClass>> registrations() {
        Map<Class<? extends NMSClass>, List<RegisteredClass>> result = new HashMap<>();
        CLASS_TO_DEQUE_MAP.forEach((key, value) -> result.put(key, List.copyOf(value)));
        return result;
    }

    static class RegisteredClass {
        public final Class<? extends NMSClass> clazz;
        public final MinecraftVersion lowerVersion;
        public final MinecraftVersion upperVersion;

        private RegisteredClass(Class<? extends NMSClass> clazz,
                                MinecraftVersion lowerVersion,
                                MinecraftVersion upperVersion) {
            this.clazz = clazz;
            this.lowerVersion = lowerVersion;
            this.upperVersion = upperVersion;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            RegisteredClass that = (RegisteredClass) o;

            if (!Objects.equals(lowerVersion, that.lowerVersion)) {
                return false;
            }
            if (!Objects.equals(upperVersion, that.upperVersion)) {
                return false;
            }
            return Objects.equals(clazz, that.clazz);
        }

        @Override
        public int hashCode() {
            int result = lowerVersion != null ? lowerVersion.hashCode() : 0;
            result = 31 * result + (upperVersion != null ? upperVersion.hashCode() : 0);
            result = 31 * result + (clazz != null ? clazz.hashCode() : 0);
            return result;
        }
    }
}
