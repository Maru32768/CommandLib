// Forge / NeoForge test mod for the Docker-based integration tests. It is built once per loader and Minecraft
// version with Stonecutter, like the modded module, and bundles the published (SRG-remapped) CommandLib jars so the
// tests exercise the same artifacts that downstream mods use.
plugins {
    id("dev.architectury.loom") version "1.17.493"
}

// The version projects are named like the modded module's (<loader>-<minecraft version>), so a distinct group keeps
// Gradle from treating :modded:<node> and this project as the same module.
group = "net.kunmc.lab.integration.forge"

val minecraftVersion = sc.current.version
val loader = property("loom.platform").toString()
val javaVersion = when {
    sc.current.parsed >= "1.20.5" -> 21
    sc.current.parsed >= "1.17" -> 17
    else -> 11
}

stonecutter {
    constants.match(loader, "forge", "neoforge")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

repositories {
    maven("https://maven.neoforged.net/releases/")
}

val bundled: Configuration by configurations.creating {
    isTransitive = true
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    if (loader == "neoforge") {
        "neoForge"("net.neoforged:neoforge:${property("deps.neoforge")}")
    } else {
        "forge"("net.minecraftforge:forge:$minecraftVersion-${property("deps.forge")}")
    }

    compileOnly(project(":common"))
    compileOnly(project(path = ":modded:${sc.current.project}", configuration = "namedElements"))
    compileOnly(project(":integration-test:shared:core"))

    bundled(project(":modded:${sc.current.project}"))
    bundled(project(":integration-test:shared:core"))
}

val forgeLoaderVersion = property("deps.forgeLoader").toString()

tasks.processResources {
    val properties = mapOf("forgeLoader" to forgeLoaderVersion)
    inputs.properties(properties)
    // NeoForge reads neoforge.mods.toml, Forge reads mods.toml.
    exclude(if (loader == "neoforge") "META-INF/mods.toml" else "META-INF/neoforge.mods.toml")
    filesMatching(listOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml")) {
        expand(properties)
    }
}

// The test mod jar that is copied into the server's mods directory.
val testModJar = tasks.register<Jar>("testModJar") {
    group = "build"
    description = "Builds the test mod jar with the remapped CommandLib jars bundled in."
    archiveFileName.set("TestMod-${sc.current.project}.jar")
    destinationDirectory.set(layout.buildDirectory.dir("test-mod"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val remapJar = tasks.named<AbstractArchiveTask>("remapJar")
    from(remapJar.flatMap { it.archiveFile }.map { zipTree(it) })
    from(bundled.elements.map { files -> files.map { zipTree(it) } }) {
        exclude("META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA", "LICENSE")
    }
}
