repositories {
    mavenCentral()
    maven {
        url = uri("https://libraries.minecraft.net")
    }
}

dependencies {
    api(project(":common"))
    api(libs.brigadier)
    compileOnlyApi(libs.annotations)
    // CommandLibExtension; tests that use it already have JUnit. 5.8.1 is the oldest docs/testing.md supports.
    compileOnly(libs.junit.jupiter.api.min)

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
