package dev.marulabs.commandlib.util.bukkit;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Pattern;

public class MinecraftVersion implements Comparable<MinecraftVersion> {
    private static final Pattern MINECRAFT_WITHOUT_PATCH_VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+");
    private static final Pattern MINECRAFT_VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+");

    private final String version;

    public static void checkMinecraftVersionFormat(String version) {
        if (!MINECRAFT_VERSION_PATTERN.matcher(version)
                                      .matches()) {
            throw new InvalidMinecraftVersionFormatException("'" + version
                    + "' doesn't satisfy the version format, '\\d+.\\d+.\\d+'");
        }
    }

    public MinecraftVersion(String version) {
        String normalized = MINECRAFT_WITHOUT_PATCH_VERSION_PATTERN.matcher(version)
                                                                   .matches() ? version + ".0" : version;
        checkMinecraftVersionFormat(normalized);
        this.version = normalized;
    }

    public boolean isWithin(MinecraftVersion lowerVersion, MinecraftVersion upperVersion) {
        int lowerRes = compareTo(lowerVersion);
        int upperRes = compareTo(upperVersion);

        return lowerRes >= 0 && upperRes <= 0;
    }

    public boolean isLessThan(MinecraftVersion other) {
        return compareTo(other) < 0;
    }

    @Override
    public int compareTo(@NotNull MinecraftVersion o) {
        String[] xUnits = version.split("\\.", -1);
        String[] yUnits = o.version.split("\\.", -1);

        for (int i = 0; i < xUnits.length; i++) {
            int xn = Integer.parseInt(xUnits[i]);
            int yn = Integer.parseInt(yUnits[i]);

            if (xn < yn) {
                return -1;
            }
            if (xn > yn) {
                return 1;
            }
        }

        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MinecraftVersion)) {
            return false;
        }

        MinecraftVersion that = (MinecraftVersion) o;

        return Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return version != null ? version.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "MinecraftVersion{" + "version='" + version + '\'' + '}';
    }

    public static class InvalidMinecraftVersionFormatException extends RuntimeException {
        public InvalidMinecraftVersionFormatException(String message) {
            super(message);
        }
    }
}
