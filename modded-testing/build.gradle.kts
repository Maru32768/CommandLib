plugins {
    id("dev.architectury.loom") version "1.17.493"
}

// The version projects are named like the modded module's (<loader>-<minecraft version>), so a distinct group keeps
// Gradle from treating :modded:<node> and this project as the same module. The published groupId is set by the
// root build.
group = "net.kunmc.lab.modded-testing"

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
    mavenCentral()
    maven("https://maven.neoforged.net/releases/")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    if (loader == "neoforge") {
        "neoForge"("net.neoforged:neoforge:${property("deps.neoforge")}")
    } else {
        "forge"("net.minecraftforge:forge:$minecraftVersion-${property("deps.forge")}")
    }

    api(project(path = ":modded:${sc.current.project}", configuration = "namedElements"))
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
