plugins {
    id("dev.architectury.loom") version "1.17.493"
}

apply(from = rootProject.file("gradle/modded-node.gradle.kts"))
val loader = extra["modded.loader"].toString()

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

if (loader == "fabric") {
    tasks.jar {
        manifest {
            // The jar is not a mod, and Loom remaps a modImplementation dependency only when it is one or opts in.
            attributes("Fabric-Loom-Remap" to "true")
        }
    }
}
