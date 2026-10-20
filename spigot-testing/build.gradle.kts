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

// The tests that run unchanged on Spigot and Paper. Their snapshots stay in each module, since Snapshots reads them
// from the project directory.
sourceSets.test {
    java.srcDir(rootProject.file("common-testing/src/bukkitTest/java"))
}

dependencies {
    api(project(":spigot"))
    compileOnly(libs.spigot.api)
    compileOnly(libs.brigadier)
    // CommandLibExtension; tests that use it already have JUnit. 5.8.1 is the oldest docs/testing.md supports.
    compileOnly(libs.junit.jupiter.api.min)
    api(libs.mockito.inline.legacy)

    testImplementation(libs.brigadier)
    testImplementation(libs.spigot.api)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Runs fixture classes to see how CommandLibExtension refuses parameters.
    testImplementation("org.junit.platform:junit-platform-testkit")
    testImplementation(libs.assertj.legacy)
}

tasks.test {
    useJUnitPlatform {
        // Run only by the tests that select them.
        excludeTags("fixture")
    }
}

// The annotated example commands take their argument names from their parameters.
tasks.compileTestJava {
    options.compilerArgs.add("-parameters")
}

// The tests run against the classes of :spigot instead of its jar, which bundles the typed NMS modules and would run
// BuildTools for every supported Spigot version. CommandTester stubs NMSClassRegistry, so the tests never load them.
configurations.testRuntimeClasspath {
    attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.CLASSES))
    }
}

dependencies {
    // The classes leave out the resources, such as the caption bundles of :common.
    testRuntimeOnly(files(rootProject.file("common/build/resources/main")).builtBy(":common:processResources"))
}
