plugins {
    id("dev.architectury.loom") version "1.17.493"
}

apply(from = rootProject.file("gradle/modded-node.gradle.kts"))
val loader = extra["modded.loader"].toString()
val minecraftVersion = extra["modded.minecraftVersion"].toString()

stonecutter {
    constants.match(loader, "forge", "neoforge", "fabric")
}

dependencies {
    api(project(":common"))
    if (loader == "fabric") {
        // Optional at runtime: CommandLib asks it only when a mod ships it (see CommandLib.hasPermission).
        "modCompileOnly"("me.lucko:fabric-permissions-api:0.3.1") { isTransitive = false }
    }
}

tasks.processResources {
    if (loader == "fabric") {
        val properties = mapOf(
            "version" to project.version.toString(),
            "minecraft" to minecraftVersion,
        )
        inputs.properties(properties)
        filesMatching("fabric.mod.json") {
            expand(properties)
        }
    } else {
        exclude("fabric.mod.json")
    }
}
