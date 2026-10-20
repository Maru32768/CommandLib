repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

dependencies {
    api(project(":velocity"))
    // CommandLibExtension; tests that use it already have JUnit. 5.8.1 is the oldest docs/testing.md supports.
    compileOnly(libs.junit.jupiter.api.min)
    api(libs.mockito.core)

    testImplementation(libs.velocity.api)
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
