pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.architectury.dev/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9.8"
}

rootProject.name = "CommandLib"
include("spigot", "paper")
// Typed NMS modules bundled into :spigot, one per directory under nms/. They need BuildTools or a paperweight dev
// bundle, so -Pcommandlib.typedNms=false leaves them out and :spigot uses only the reflection implementations, and
// -Pcommandlib.typedNms=spigot-1.20.4,paper-1.20.6 includes only the listed modules.
val typedNmsModules = file("nms").listFiles()
    .orEmpty()
    .filter { it.resolve("build.gradle.kts").isFile }
    .map { it.name }
    .sorted()
// NMSClassRegistry only looks up modules listed in the table that tools/nms-codegen/generate.py writes.
val typedNmsTable = Regex("""new NMSClassRegistry\.TypedModule\("([^"]+)"""")
    .findAll(file("spigot/src/main/java/net/kunmc/lab/commandlib/util/nms/TypedNmsModules.java").readText())
    .map { it.groupValues[1] }
    .toSet()
typedNmsModules.forEach {
    val modulePackage = it.replace("-", "_").replace(".", "_")
    require(modulePackage in typedNmsTable) {
        "nms/$it is missing from TypedNmsModules. Add it to MODULES in tools/nms-codegen/generate.py and regenerate."
    }
}
val typedNmsSelection = providers.gradleProperty("commandlib.typedNms").orNull
val includedTypedNmsModules = when (typedNmsSelection) {
    null, "true" -> typedNmsModules
    "false" -> emptyList()
    else -> typedNmsSelection.split(",").map(String::trim).onEach {
        require(it in typedNmsModules) { "Unknown typed NMS module in commandlib.typedNms: $it" }
    }
}
includedTypedNmsModules.forEach { include("nms:$it") }
include("common")
include("common-testing", "spigot-testing", "paper-testing")
// Forge and NeoForge share one source tree. Nodes are named <loader>-<minecraft version>.
include("modded")
include("modded-testing")
stonecutter {
    create("modded") {
        version("forge-1.16.5", "1.16.5")
        version("forge-1.20.1", "1.20.1")
        version("neoforge-1.21.1", "1.21.1")
        vcsVersion = "forge-1.20.1"
    }
    create("modded-testing") {
        version("forge-1.16.5", "1.16.5")
        version("forge-1.20.1", "1.20.1")
        version("neoforge-1.21.1", "1.21.1")
        vcsVersion = "forge-1.20.1"
    }
}
include("integration-test")
include(
    "integration-test:shared:core",
    "integration-test:shared:spigot-fixture",
    "integration-test:shared:paper-fixture",
)
project(":integration-test:shared:spigot-fixture").projectDir = file("integration-test/shared/spigot")
project(":integration-test:shared:paper-fixture").projectDir = file("integration-test/shared/paper")
include("integration-test:shared:modded-fixture")
project(":integration-test:shared:modded-fixture").projectDir = file("integration-test/shared/modded")
stonecutter {
    create(":integration-test:shared:modded-fixture") {
        version("forge-1.16.5", "1.16.5")
        version("forge-1.20.1", "1.20.1")
        version("neoforge-1.21.1", "1.21.1")
        vcsVersion = "forge-1.20.1"
    }
}
include(
    "integration-test:targets:paper-1.16.5",
    "integration-test:targets:mohist-1.16.5",
    "integration-test:targets:paper-1.17.1",
    "integration-test:targets:paper-1.18.2",
    "integration-test:targets:paper-1.19.2",
    "integration-test:targets:paper-1.19.4",
    "integration-test:targets:paper-1.20.1",
    "integration-test:targets:mohist-1.20.1",
    "integration-test:targets:paper-1.20.4",
    "integration-test:targets:paper-1.20.5",
    "integration-test:targets:paper-1.20.6",
    "integration-test:targets:paper-1.21.0",
    "integration-test:targets:forge-1.16.5",
    "integration-test:targets:forge-1.20.1",
    "integration-test:targets:neoforge-1.21.1",
    "integration-test:targets:spigot-1.20.4",
    "integration-test:targets:spigot-1.20.6",
    "integration-test:targets:paper-1.18",
    "integration-test:targets:paper-1.18.1",
    "integration-test:targets:paper-1.19",
    "integration-test:targets:paper-1.19.1",
    "integration-test:targets:paper-1.19.3",
    "integration-test:targets:paper-1.20",
    "integration-test:targets:paper-1.20.2",
    "integration-test:targets:spigot-1.21.1",
    "integration-test:targets:spigot-1.20.5",
    "integration-test:targets:spigot-1.21",
)
