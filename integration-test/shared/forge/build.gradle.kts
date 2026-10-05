// Forge test mod for the Docker-based integration tests. It is built once per Minecraft version with
// Stonecutter, like the forge module, and bundles the published (SRG-remapped) CommandLib jars so the
// tests exercise the same artifacts that downstream mods use.
plugins {
    id("dev.architectury.loom") version "1.17.493"
}

// The version projects are named after the Minecraft version like the forge module's, so a distinct group keeps
// Gradle from treating :forge:<version> and this project as the same module.
group = "net.kunmc.lab.integration.forge"

val minecraftVersion = sc.current.version
val javaVersion = if (sc.current.parsed >= "1.17") 17 else 11

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

val bundled: Configuration by configurations.creating {
    isTransitive = true
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    forge("net.minecraftforge:forge:$minecraftVersion-${property("deps.forge")}")

    compileOnly(project(":common"))
    compileOnly(project(path = ":forge:$minecraftVersion", configuration = "namedElements"))
    compileOnly(project(":integration-test:shared:core"))

    bundled(project(":forge:$minecraftVersion"))
    bundled(project(":integration-test:shared:core"))
}

val forgeLoaderVersion = property("deps.forgeLoader").toString()

tasks.processResources {
    val properties = mapOf("forgeLoader" to forgeLoaderVersion)
    inputs.properties(properties)
    filesMatching("META-INF/mods.toml") {
        expand(properties)
    }
}

// The test mod jar that is copied into the server's mods directory.
val testModJar = tasks.register<Jar>("testModJar") {
    group = "build"
    description = "Builds the test mod jar with the remapped CommandLib jars bundled in."
    archiveFileName.set("TestMod-forge-$minecraftVersion.jar")
    destinationDirectory.set(layout.buildDirectory.dir("test-mod"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val remapJar = tasks.named<AbstractArchiveTask>("remapJar")
    from(remapJar.flatMap { it.archiveFile }.map { zipTree(it) })
    from(bundled.elements.map { files -> files.map { zipTree(it) } }) {
        exclude("META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA", "LICENSE")
    }
}
