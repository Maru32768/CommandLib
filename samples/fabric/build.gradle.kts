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

// -PcommandlibLocal=true builds against this repository's code instead of the jitpack release. Publish it first with
// ./gradlew -Pcommandlib.typedNms=false :common:publishToMavenLocal :modded:fabric-1.21.1:publishToMavenLocal
// at the repository root.
val commandLibLocal = providers.gradleProperty("commandlibLocal").orNull.toBoolean()
val commandLibGroup = if (commandLibLocal) "dev.marulabs" else "com.github.Maru32768.CommandLib"

repositories {
    if (commandLibLocal) {
        mavenLocal()
    }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.1")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.16.14")
    // CommandLib needs Fabric API's command and lifecycle events.
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.116.17+1.21.1")
    // modImplementation remaps CommandLib to the development names, so the shadow jar below can take it from the
    // runtime classpath, and remapJar maps the mod and its relocated copy of CommandLib to intermediary together.
    modImplementation("$commandLibGroup:fabric-1.21.1:latest.release")
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
        include { it.moduleGroup.endsWith(commandLibGroup) }
    }
    // The copy is not a mod of its own: keep this mod's fabric.mod.json and leave CommandLib's out.
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    relocate("dev.marulabs.commandlib", "$projectGroup.$projectNameLower.commandlib")
}

tasks.named<RemapJarTask>("remapJar") {
    inputFile.set(shadowJar.flatMap { it.archiveFile })
}
