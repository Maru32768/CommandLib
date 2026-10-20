repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// The tests that run unchanged on Spigot and Paper. Their snapshots stay in each module, since Snapshots reads them
// from the project directory.
sourceSets.test {
    java.srcDir(rootProject.file("common-testing/src/bukkitTest/java"))
}

dependencies {
    api(project(":paper"))
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly("com.mojang:brigadier:1.0.18")
    // CommandLibExtension; tests that use it already have JUnit. 5.8.1 is the oldest docs/testing.md supports.
    compileOnly("org.junit.jupiter:junit-jupiter-api:5.8.1")
    api("org.mockito:mockito-core:5.11.0")

    // 1.21.8 added ArgumentTypes.blockInWorldPredicate(), which CommandTester stubs when the API has it.
    testImplementation("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    testImplementation("com.mojang:brigadier:1.0.18")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Runs fixture classes to see how CommandLibExtension refuses parameters.
    testImplementation("org.junit.platform:junit-platform-testkit")
    testImplementation("org.assertj:assertj-core:3.25.3")
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
