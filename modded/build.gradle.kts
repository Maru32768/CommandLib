plugins {
    id("dev.architectury.loom") version "1.17.493"
}

apply(from = rootProject.file("gradle/modded-node.gradle.kts"))
val loader = extra["modded.loader"].toString()

stonecutter {
    constants.match(loader, "forge", "neoforge")
}

dependencies {
    api(project(":common"))
}
