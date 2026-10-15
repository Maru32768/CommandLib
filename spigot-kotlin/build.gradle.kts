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
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("com.mojang:brigadier:1.0.18")

    testImplementation(project(":spigot-testing"))
    testImplementation("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    testImplementation("com.mojang:brigadier:1.0.18")
}

// As in spigot-testing: the classes of :spigot, not its jar with the typed NMS modules, which CommandTester stubs.
configurations.testRuntimeClasspath {
    attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.CLASSES))
    }
}
