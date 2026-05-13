pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "CommandLib"
include("forge", "spigot", "paper")
include("common")
include("common-testing", "spigot-testing", "paper-testing")
include("integration-test")
include(
    "integration-test:shared:core",
    "integration-test:shared:spigot-fixture",
    "integration-test:shared:paper-fixture",
)
project(":integration-test:shared:spigot-fixture").projectDir = file("integration-test/shared/spigot")
project(":integration-test:shared:paper-fixture").projectDir = file("integration-test/shared/paper")
include(
    "integration-test:targets:paper-1.16.5",
    "integration-test:targets:mohist-1.16.5",
    "integration-test:targets:paper-1.19.4",
    "integration-test:targets:paper-1.20.1",
    "integration-test:targets:mohist-1.20.1",
    "integration-test:targets:paper-1.20.4",
    "integration-test:targets:paper-1.20.5",
    "integration-test:targets:paper-1.20.6",
    "integration-test:targets:paper-1.21.0",
)
