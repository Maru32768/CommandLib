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
)
