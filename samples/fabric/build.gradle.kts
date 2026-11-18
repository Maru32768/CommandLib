import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask

plugins {
    java
    // Architectury Loom, as the repository builds with; the Fabric Loom setup is the same.
    id("dev.architectury.loom") version "1.17.493"
    id("com.gradleup.shadow") version "9.4.1"
}

group = "dev.marulabs"
version = "1.0.0"

base.archivesName.set("samplefabric")

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// -PcadenceLocal=true builds against this repository's code instead of the jitpack release. Publish it first with
// ./gradlew -Pcadence.typedNms=false :common:publishToMavenLocal :modded:fabric-1.21.1:publishToMavenLocal
// at the repository root.
val cadenceLocal = providers.gradleProperty("cadenceLocal").orNull.toBoolean()
val cadenceGroup = if (cadenceLocal) "dev.marulabs" else "com.github.Maru32768.Cadence"

repositories {
    if (cadenceLocal) {
        mavenLocal()
    }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.1")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.16.14")
    // Cadence needs Fabric API's command and lifecycle events.
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.116.17+1.21.1")
    // modImplementation remaps Cadence to the development names, so the shadow jar below can take it from the
    // runtime classpath, and remapJar maps the mod and its relocated copy of Cadence to intermediary together.
    modImplementation("$cadenceGroup:fabric-1.21.1:latest.release")
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}

val projectGroup = project.group.toString()
val projectNameLower = project.name.lowercase()
val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("dev-shadow")
    dependencies {
        // The platform artifact and the common artifact it depends on, nothing of Minecraft or Fabric. Loom puts the
        // remapped platform artifact in the group remapped.<group>, so match the end of the group.
        include { it.moduleGroup.endsWith(cadenceGroup) }
    }
    relocate("dev.marulabs.cadence", "$projectGroup.$projectNameLower.cadence")
}

tasks.named<RemapJarTask>("remapJar") {
    inputFile.set(shadowJar.flatMap { it.archiveFile })
}
