plugins {
    id("dev.architectury.loom") version "1.17.493"
}

// The version projects are named like the modded module's (<loader>-<minecraft version>), so a distinct group keeps
// Gradle from treating :modded:<node> and this project as the same module. The published groupId is set by the
// root build.
group = "net.kunmc.lab.modded-testing"

apply(from = rootProject.file("gradle/modded-node.gradle.kts"))
val loader = extra["modded.loader"].toString()

stonecutter {
    constants.match(loader, "forge", "neoforge")
}

repositories {
    mavenCentral()
}

dependencies {
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
