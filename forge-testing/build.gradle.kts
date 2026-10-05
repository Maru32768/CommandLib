plugins {
    id("dev.architectury.loom") version "1.17.493"
}

// The version projects are named after the Minecraft version like the forge module's, so a distinct group keeps
// Gradle from treating :forge:<version> and this project as the same module. The published groupId is set by the
// root build.
group = "net.kunmc.lab.forge-testing"

val minecraftVersion = sc.current.version
val javaVersion = if (sc.current.parsed >= "1.17") 17 else 11

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    forge("net.minecraftforge:forge:$minecraftVersion-${property("deps.forge")}")

    api(project(path = ":forge:$minecraftVersion", configuration = "namedElements"))
    api("org.mockito:mockito-core:5.11.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.25.3")
}

tasks.test {
    useJUnitPlatform()
    // Minecraft writes logs relative to the working directory while bootstrapping.
    workingDir = layout.buildDirectory.dir("test-run").get().asFile.also { it.mkdirs() }
}
