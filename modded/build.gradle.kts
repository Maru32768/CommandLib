import java.util.jar.Manifest
import java.util.zip.ZipFile

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
        // Optional at runtime: Cadence asks it only when a mod ships it (see Cadence.hasPermission).
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

    val checkPublishedJar = tasks.register("checkPublishedJar") {
        group = "verification"
        description = "Checks that the published jar is a plain library that Loom remaps for modImplementation."
        val jar = tasks.named<AbstractArchiveTask>("remapJar").flatMap { it.archiveFile }
        inputs.file(jar)
        doLast {
            ZipFile(jar.get().asFile).use { zip ->
                check(zip.getEntry("fabric.mod.json") == null) { "${zip.name} has a fabric.mod.json" }
                val manifest = zip.getInputStream(zip.getEntry("META-INF/MANIFEST.MF")).use { Manifest(it) }
                check(manifest.mainAttributes.getValue("Fabric-Loom-Remap") == "true") {
                    "${zip.name} lacks Fabric-Loom-Remap: true"
                }
            }
        }
    }
    tasks.check {
        dependsOn(checkPublishedJar)
    }
}
