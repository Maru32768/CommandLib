// Forge / NeoForge / Fabric test mod for the Docker-based integration tests. It is built once per loader and Minecraft
// version with Stonecutter, like the modded module, and bundles the published CommandLib jars (remapped to SRG on
// Forge, Mojang names on NeoForge, intermediary on Fabric) so the tests exercise the same artifacts that downstream
// mods use.
plugins {
    id("dev.architectury.loom") version "1.17.493"
}

// The version projects are named like the modded module's (<loader>-<minecraft version>), so a distinct group keeps
// Gradle from treating :modded:<node> and this project as the same module.
group = "dev.marulabs.integration.modded"

apply(from = rootProject.file("gradle/modded-node.gradle.kts"))
val loader = extra["modded.loader"].toString()

stonecutter {
    constants.match(loader, "forge", "neoforge", "fabric")
}

val bundled: Configuration by configurations.creating {
    isTransitive = true
}

dependencies {
    compileOnly(project(":common"))
    compileOnly(project(path = ":modded:${sc.current.project}", configuration = "namedElements"))
    compileOnly(project(":integration-test:shared:core"))

    if (loader == "fabric") {
        // The CommandLib jar is a Fabric mod with its own fabric.mod.json and entrypoint, so it is nested (jar-in-jar)
        // as a downstream mod ships it, together with common, instead of being merged into the test mod.
        "include"(project(":modded:${sc.current.project}"))
        "include"(project(":common"))
    } else {
        bundled(project(":modded:${sc.current.project}"))
    }
    bundled(project(":integration-test:shared:core"))
}

val metadataFiles = mapOf(
    "forge" to "META-INF/mods.toml",
    "neoforge" to "META-INF/neoforge.mods.toml",
    "fabric" to "fabric.mod.json",
)

tasks.processResources {
    exclude(metadataFiles.filterKeys { it != loader }.values)
    if (loader != "fabric") {
        val properties = mapOf("forgeLoader" to project.property("deps.forgeLoader").toString())
        inputs.properties(properties)
        filesMatching(metadataFiles.getValue(loader)) {
            expand(properties)
        }
    }
}

// The test mod jar that is copied into the server's mods directory.
val testModJar = tasks.register<Jar>("testModJar") {
    group = "build"
    description = "Builds the test mod jar with the remapped CommandLib jars bundled or nested in."
    archiveFileName.set("TestMod-${sc.current.project}.jar")
    destinationDirectory.set(layout.buildDirectory.dir("test-mod"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val remapJar = tasks.named<AbstractArchiveTask>("remapJar")
    from(remapJar.flatMap { it.archiveFile }.map { zipTree(it) })
    from(bundled.elements.map { files -> files.map { zipTree(it) } }) {
        exclude("META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA", "LICENSE")
    }
}
