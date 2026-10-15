// The comparison examples of docs/comparison.md: each use case written in the fluent, Java builder, annotation and
// Kotlin DSL styles, and tests that check that the styles give the same tree, help and results.
plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven {
        name = "spigotmc-repo"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        url = uri("https://libraries.minecraft.net")
    }
}

dependencies {
    implementation(project(":spigot-kotlin"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("com.mojang:brigadier:1.0.18")

    testImplementation(project(":spigot-testing"))
    testImplementation("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    testImplementation("com.mojang:brigadier:1.0.18")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.25.3")
}

// The annotated examples take their argument names from their parameters.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

tasks.test {
    useJUnitPlatform()
}

// As in spigot-testing: the classes of :spigot, not its jar with the typed NMS modules, which CommandTester stubs.
configurations.testRuntimeClasspath {
    attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.CLASSES))
    }
}

dependencies {
    // The classes leave out the resources, such as the caption bundles of :common.
    testRuntimeOnly(files(rootProject.file("common/build/resources/main")).builtBy(":common:processResources"))
}
