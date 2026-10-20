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
    compileOnly(libs.paper.api)
    compileOnly(libs.brigadier)
    // CommandLibExtension; tests that use it already have JUnit. 5.8.1 is the oldest docs/testing.md supports.
    compileOnly(libs.junit.jupiter.api.min)
    api(libs.mockito.core)

    // 1.21.8 added ArgumentTypes.blockInWorldPredicate(), which CommandTester stubs when the API has it.
    testImplementation(libs.paper.api.test)
    testImplementation(libs.brigadier)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Runs fixture classes to see how CommandLibExtension refuses parameters.
    testImplementation("org.junit.platform:junit-platform-testkit")
    testImplementation(libs.assertj)
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
