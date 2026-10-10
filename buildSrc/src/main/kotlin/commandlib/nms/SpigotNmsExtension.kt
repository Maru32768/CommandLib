package commandlib.nms

import org.gradle.api.provider.Property

abstract class SpigotNmsExtension {
    /** Minecraft version of the server jar, such as `1.20.4`. */
    abstract val minecraftVersion: Property<String>

    /**
     * Passed to BuildTools as `--rev`. Defaults to [minecraftVersion]. Set a Spigot build number for a release that
     * BuildTools now resolves to a later build, such as 1.21, which resolves to the 1.21.1 build.
     */
    abstract val buildToolsRevision: Property<String>

    /**
     * Compiles against the Mojang-mapped server jar and remaps the built jar to Spigot names. Required from 1.17,
     * where the Spigot-mapped server jar keeps obfuscated member names.
     */
    abstract val remapped: Property<Boolean>
}

/** Java release that BuildTools needs to build the given Minecraft version, and that the server runs on. */
fun buildToolsJavaVersion(minecraftVersion: String): Int {
    val parts = minecraftVersion.split(".").map { it.toIntOrNull() ?: 0 }
    val major = parts.getOrElse(0) { 0 }
    val minor = parts.getOrElse(1) { 0 }
    val patch = parts.getOrElse(2) { 0 }
    return when {
        major > 1 -> 25
        minor < 17 -> 8
        minor == 17 -> 16
        minor < 20 || (minor == 20 && patch < 5) -> 17
        else -> 21
    }
}

/** Spigot renamed methods with its own mappings up to 1.17 and keeps obfuscated member names after. */
internal fun hasSpigotMemberMappings(minecraftVersion: String): Boolean {
    val parts = minecraftVersion.split(".").map { it.toIntOrNull() ?: 0 }
    return parts.getOrElse(0) { 0 } == 1 && parts.getOrElse(1) { 0 } < 18
}
