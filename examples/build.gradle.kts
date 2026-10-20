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
    implementation(libs.kotlinx.coroutines.core)
    compileOnly(libs.spigot.api)
    compileOnly(libs.brigadier)

    testImplementation(project(":spigot-testing"))
    testImplementation(libs.spigot.api)
    testImplementation(libs.brigadier)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj)
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
