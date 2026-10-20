repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

// Velocity 3.4 runs on Java 17. The jar compiled against its API also loads on later Velocity releases.
configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

// The Adventure converter and its test are the same on Paper and Velocity.
sourceSets.main {
    java.srcDir(rootProject.file("common/src/adventure/java"))
}
sourceSets.test {
    java.srcDir(rootProject.file("common/src/adventureTest/java"))
}

dependencies {
    api(project(":common"))
    // Brings Velocity's Brigadier fork and Adventure.
    compileOnlyApi(libs.velocity.api)
    compileOnlyApi(libs.annotations)

    testImplementation(libs.velocity.api)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj)
}

tasks.test {
    useJUnitPlatform()
}
