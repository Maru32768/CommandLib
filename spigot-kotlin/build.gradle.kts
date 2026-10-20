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

// The command factories and their tests are the same on every platform.
sourceSets.main {
    kotlin.srcDir(rootProject.file("common-kotlin/src/platform/kotlin"))
}
sourceSets.test {
    kotlin.srcDir(rootProject.file("common-kotlin/src/platformTest/kotlin"))
}

dependencies {
    api(project(":common-kotlin"))
    api(project(":spigot"))
    compileOnly(libs.spigot.api)
    compileOnly(libs.brigadier)

    testImplementation(project(":spigot-testing"))
    testImplementation(libs.spigot.api)
    testImplementation(libs.brigadier)
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
