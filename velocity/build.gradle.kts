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
    compileOnlyApi("com.velocitypowered:velocity-api:3.4.0")
    compileOnlyApi("org.jetbrains:annotations:16.0.2")

    testImplementation("com.velocitypowered:velocity-api:3.4.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.25.3")
}

tasks.test {
    useJUnitPlatform()
}
