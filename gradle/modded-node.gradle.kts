/*
 * Shared setup for the Forge / NeoForge Stonecutter nodes: :modded, :modded-testing and the integration test mod.
 * Apply it right after the Architectury Loom plugin:
 *
 *   apply(from = rootProject.file("gradle/modded-node.gradle.kts"))
 *   val loader = extra["modded.loader"].toString()
 *
 * Nodes are named <loader>-<minecraft version>. The loader build of every node is read from
 * modded/versions/<node>/gradle.properties, so the library, the testing artifact, the test mod and the integration
 * server are all built against the same Forge / NeoForge build.
 */

import java.util.Properties

val node = project.name
val nodeProperties = Properties().apply {
    rootProject.file("modded/versions/$node/gradle.properties")
        .reader()
        .use { load(it) }
}
val loader = nodeProperties.getProperty("loom.platform")
check(loader == property("loom.platform").toString()) {
    "loom.platform of $path must match modded/versions/$node/gradle.properties ($loader)."
}
val minecraftVersion = node.substringAfter('-')

fun compareVersions(a: String, b: String): Int {
    val left = a.split('.').map { it.toInt() }
    val right = b.split('.').map { it.toInt() }
    for (i in 0 until maxOf(left.size, right.size)) {
        val diff = left.getOrElse(i) { 0 } - right.getOrElse(i) { 0 }
        if (diff != 0) {
            return diff
        }
    }
    return 0
}

val javaVersion = when {
    compareVersions(minecraftVersion, "1.20.5") >= 0 -> 21
    compareVersions(minecraftVersion, "1.17") >= 0 -> 17
    else -> 11
}

extra["modded.loader"] = loader
extra["modded.minecraftVersion"] = minecraftVersion

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

repositories {
    maven("https://maven.neoforged.net/releases/")
}

// Script plugins cannot see Loom's classes, so call loom.officialMojangMappings() reflectively.
val loom = extensions.getByName("loom")
val officialMojangMappings = loom.javaClass
    .getMethod("officialMojangMappings")
    .invoke(loom)

dependencies {
    "minecraft"("com.mojang:minecraft:$minecraftVersion")
    "mappings"(officialMojangMappings)
    if (loader == "neoforge") {
        "neoForge"("net.neoforged:neoforge:${nodeProperties.getProperty("deps.neoforge")}")
    } else {
        "forge"("net.minecraftforge:forge:$minecraftVersion-${nodeProperties.getProperty("deps.forge")}")
    }
}
